package com.android.internal.util.function.pooled;

import java.util.function.Predicate;
/* loaded from: classes4.dex */
public interface PooledPredicate<T> extends PooledLambda, Predicate<T> {
    PooledConsumer<T> asConsumer();

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.util.function.pooled.PooledSupplier, com.android.internal.util.function.pooled.PooledRunnable, com.android.internal.util.function.pooled.PooledSupplier.OfInt, com.android.internal.util.function.pooled.PooledSupplier.OfLong, com.android.internal.util.function.pooled.PooledSupplier.OfDouble
    PooledPredicate<T> recycleOnUse();
}
