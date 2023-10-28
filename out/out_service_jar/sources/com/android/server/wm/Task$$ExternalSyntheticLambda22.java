package com.android.server.wm;

import java.util.function.Predicate;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class Task$$ExternalSyntheticLambda22 implements Predicate {
    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return ((ActivityRecord) obj).canBeTopRunning();
    }
}
