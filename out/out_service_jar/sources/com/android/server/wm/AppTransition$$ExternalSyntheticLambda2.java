package com.android.server.wm;

import java.util.function.BiPredicate;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class AppTransition$$ExternalSyntheticLambda2 implements BiPredicate {
    @Override // java.util.function.BiPredicate
    public final boolean test(Object obj, Object obj2) {
        return ((Task) obj).isTaskId(((Integer) obj2).intValue());
    }
}
