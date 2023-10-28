package com.android.server.wm;

import android.view.SurfaceControl;
import com.android.server.wm.SurfaceAnimator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class SimpleSurfaceAnimatable implements SurfaceAnimator.Animatable {
    private final Supplier<SurfaceControl.Builder> mAnimationLeashFactory;
    private final SurfaceControl mAnimationLeashParent;
    private final Runnable mCommitTransactionRunnable;
    private final int mHeight;
    private final Consumer<Runnable> mOnAnimationFinished;
    private final BiConsumer<SurfaceControl.Transaction, SurfaceControl> mOnAnimationLeashCreated;
    private final Consumer<SurfaceControl.Transaction> mOnAnimationLeashLost;
    private final SurfaceControl mParentSurfaceControl;
    private final Supplier<SurfaceControl.Transaction> mPendingTransaction;
    private final boolean mShouldDeferAnimationFinish;
    private final SurfaceControl mSurfaceControl;
    private final Supplier<SurfaceControl.Transaction> mSyncTransaction;
    private final int mWidth;

    private SimpleSurfaceAnimatable(Builder builder) {
        this.mWidth = builder.mWidth;
        this.mHeight = builder.mHeight;
        this.mShouldDeferAnimationFinish = builder.mShouldDeferAnimationFinish;
        this.mAnimationLeashParent = builder.mAnimationLeashParent;
        this.mSurfaceControl = builder.mSurfaceControl;
        this.mParentSurfaceControl = builder.mParentSurfaceControl;
        this.mCommitTransactionRunnable = builder.mCommitTransactionRunnable;
        this.mAnimationLeashFactory = builder.mAnimationLeashFactory;
        this.mOnAnimationLeashCreated = builder.mOnAnimationLeashCreated;
        this.mOnAnimationLeashLost = builder.mOnAnimationLeashLost;
        this.mSyncTransaction = builder.mSyncTransactionSupplier;
        this.mPendingTransaction = builder.mPendingTransactionSupplier;
        this.mOnAnimationFinished = builder.mOnAnimationFinished;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Transaction getSyncTransaction() {
        return this.mSyncTransaction.get();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Transaction getPendingTransaction() {
        return this.mPendingTransaction.get();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void commitPendingTransaction() {
        this.mCommitTransactionRunnable.run();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        BiConsumer<SurfaceControl.Transaction, SurfaceControl> biConsumer = this.mOnAnimationLeashCreated;
        if (biConsumer != null) {
            biConsumer.accept(t, leash);
        }
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        Consumer<SurfaceControl.Transaction> consumer = this.mOnAnimationLeashLost;
        if (consumer != null) {
            consumer.accept(t);
        }
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Builder makeAnimationLeash() {
        return this.mAnimationLeashFactory.get();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getAnimationLeashParent() {
        return this.mAnimationLeashParent;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getParentSurfaceControl() {
        return this.mParentSurfaceControl;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public int getSurfaceWidth() {
        return this.mWidth;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public int getSurfaceHeight() {
        return this.mHeight;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
        Consumer<Runnable> consumer = this.mOnAnimationFinished;
        if (consumer != null) {
            consumer.accept(endDeferFinishCallback);
        }
        return this.mShouldDeferAnimationFinish;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Builder {
        private Supplier<SurfaceControl.Builder> mAnimationLeashFactory;
        private Runnable mCommitTransactionRunnable;
        private Supplier<SurfaceControl.Transaction> mPendingTransactionSupplier;
        private Supplier<SurfaceControl.Transaction> mSyncTransactionSupplier;
        private int mWidth = -1;
        private int mHeight = -1;
        private boolean mShouldDeferAnimationFinish = false;
        private SurfaceControl mAnimationLeashParent = null;
        private SurfaceControl mSurfaceControl = null;
        private SurfaceControl mParentSurfaceControl = null;
        private BiConsumer<SurfaceControl.Transaction, SurfaceControl> mOnAnimationLeashCreated = null;
        private Consumer<SurfaceControl.Transaction> mOnAnimationLeashLost = null;
        private Consumer<Runnable> mOnAnimationFinished = null;

        public Builder setCommitTransactionRunnable(Runnable commitTransactionRunnable) {
            this.mCommitTransactionRunnable = commitTransactionRunnable;
            return this;
        }

        public Builder setOnAnimationLeashCreated(BiConsumer<SurfaceControl.Transaction, SurfaceControl> onAnimationLeashCreated) {
            this.mOnAnimationLeashCreated = onAnimationLeashCreated;
            return this;
        }

        public Builder setOnAnimationLeashLost(Consumer<SurfaceControl.Transaction> onAnimationLeashLost) {
            this.mOnAnimationLeashLost = onAnimationLeashLost;
            return this;
        }

        public Builder setSyncTransactionSupplier(Supplier<SurfaceControl.Transaction> syncTransactionSupplier) {
            this.mSyncTransactionSupplier = syncTransactionSupplier;
            return this;
        }

        public Builder setPendingTransactionSupplier(Supplier<SurfaceControl.Transaction> pendingTransactionSupplier) {
            this.mPendingTransactionSupplier = pendingTransactionSupplier;
            return this;
        }

        public Builder setAnimationLeashSupplier(Supplier<SurfaceControl.Builder> animationLeashFactory) {
            this.mAnimationLeashFactory = animationLeashFactory;
            return this;
        }

        public Builder setAnimationLeashParent(SurfaceControl animationLeashParent) {
            this.mAnimationLeashParent = animationLeashParent;
            return this;
        }

        public Builder setSurfaceControl(SurfaceControl surfaceControl) {
            this.mSurfaceControl = surfaceControl;
            return this;
        }

        public Builder setParentSurfaceControl(SurfaceControl parentSurfaceControl) {
            this.mParentSurfaceControl = parentSurfaceControl;
            return this;
        }

        public Builder setWidth(int width) {
            this.mWidth = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.mHeight = height;
            return this;
        }

        public Builder setShouldDeferAnimationFinish(boolean shouldDeferAnimationFinish, Consumer<Runnable> onAnimationFinish) {
            this.mShouldDeferAnimationFinish = shouldDeferAnimationFinish;
            this.mOnAnimationFinished = onAnimationFinish;
            return this;
        }

        public SurfaceAnimator.Animatable build() {
            if (this.mSyncTransactionSupplier == null) {
                throw new IllegalArgumentException("mSyncTransactionSupplier cannot be null");
            }
            if (this.mPendingTransactionSupplier == null) {
                throw new IllegalArgumentException("mPendingTransactionSupplier cannot be null");
            }
            if (this.mAnimationLeashFactory == null) {
                throw new IllegalArgumentException("mAnimationLeashFactory cannot be null");
            }
            if (this.mCommitTransactionRunnable == null) {
                throw new IllegalArgumentException("mCommitTransactionRunnable cannot be null");
            }
            if (this.mSurfaceControl == null) {
                throw new IllegalArgumentException("mSurfaceControl cannot be null");
            }
            return new SimpleSurfaceAnimatable(this);
        }
    }
}
