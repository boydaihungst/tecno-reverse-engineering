package com.android.internal.util.function.pooled;

import java.util.function.Function;
/* loaded from: classes4.dex */
public interface PooledFunction<A, R> extends PooledLambda, Function<A, R> {
    PooledConsumer<A> asConsumer();

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.util.function.pooled.PooledLambda, com.android.internal.util.function.pooled.PooledConsumer, com.android.internal.util.function.pooled.PooledPredicate, com.android.internal.util.function.pooled.PooledSupplier, com.android.internal.util.function.pooled.PooledRunnable, com.android.internal.util.function.pooled.PooledSupplier.OfInt, com.android.internal.util.function.pooled.PooledSupplier.OfLong, com.android.internal.util.function.pooled.PooledSupplier.OfDouble
    PooledFunction<A, R> recycleOnUse();
}
