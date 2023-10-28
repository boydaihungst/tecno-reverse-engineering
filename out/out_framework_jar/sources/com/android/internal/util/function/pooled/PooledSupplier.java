package com.android.internal.util.function.pooled;

import com.android.internal.util.FunctionalUtils;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface PooledSupplier<T> extends PooledLambda, Supplier<T>, FunctionalUtils.ThrowingSupplier<T> {

    /* loaded from: classes4.dex */
    public interface OfDouble extends DoubleSupplier, PooledLambda {
        /* JADX DEBUG: Method merged with bridge method */
        @Override // 
        OfDouble recycleOnUse();
    }

    /* loaded from: classes4.dex */
    public interface OfInt extends IntSupplier, PooledLambda {
        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.internal.util.function.pooled.PooledSupplier.OfLong, com.android.internal.util.function.pooled.PooledSupplier.OfDouble
        OfInt recycleOnUse();
    }

    /* loaded from: classes4.dex */
    public interface OfLong extends LongSupplier, PooledLambda {
        /* JADX DEBUG: Method merged with bridge method */
        @Override // com.android.internal.util.function.pooled.PooledSupplier.OfDouble
        OfLong recycleOnUse();
    }

    PooledRunnable asRunnable();

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.util.function.pooled.PooledRunnable, com.android.internal.util.function.pooled.PooledSupplier.OfInt, com.android.internal.util.function.pooled.PooledSupplier.OfLong, com.android.internal.util.function.pooled.PooledSupplier.OfDouble
    PooledSupplier<T> recycleOnUse();
}
