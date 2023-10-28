package com.android.server.timezonedetector;

import android.util.ArraySet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
/* loaded from: classes2.dex */
class OrdinalGenerator<T> {
    private final Function<T, T> mCanonicalizationFunction;
    private final ArraySet<T> mKnownIds = new ArraySet<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public OrdinalGenerator(Function<T, T> canonicalizationFunction) {
        this.mCanonicalizationFunction = (Function) Objects.requireNonNull(canonicalizationFunction);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int ordinal(T object) {
        T canonical = this.mCanonicalizationFunction.apply(object);
        int ordinal = this.mKnownIds.indexOf(canonical);
        if (ordinal < 0) {
            int ordinal2 = this.mKnownIds.size();
            this.mKnownIds.add(canonical);
            return ordinal2;
        }
        return ordinal;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] ordinals(List<T> objects) {
        int[] ordinals = new int[objects.size()];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = ordinal(objects.get(i));
        }
        return ordinals;
    }
}
