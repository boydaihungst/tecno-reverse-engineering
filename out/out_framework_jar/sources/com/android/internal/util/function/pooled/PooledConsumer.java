package com.android.internal.util.function.pooled;

import java.util.function.Consumer;
/* loaded from: classes4.dex */
public interface PooledConsumer<T> extends PooledLambda, Consumer<T> {
    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.util.function.pooled.PooledLambda, com.android.internal.util.function.pooled.PooledConsumer, com.android.internal.util.function.pooled.PooledPredicate, com.android.internal.util.function.pooled.PooledSupplier, com.android.internal.util.function.pooled.PooledRunnable, com.android.internal.util.function.pooled.PooledSupplier.OfInt, com.android.internal.util.function.pooled.PooledSupplier.OfLong, com.android.internal.util.function.pooled.PooledSupplier.OfDouble
    PooledConsumer<T> recycleOnUse();
}
