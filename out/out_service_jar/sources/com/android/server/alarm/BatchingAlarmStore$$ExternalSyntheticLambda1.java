package com.android.server.alarm;

import java.util.function.ToLongFunction;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes.dex */
public final /* synthetic */ class BatchingAlarmStore$$ExternalSyntheticLambda1 implements ToLongFunction {
    @Override // java.util.function.ToLongFunction
    public final long applyAsLong(Object obj) {
        return ((Alarm) obj).getWhenElapsed();
    }
}
