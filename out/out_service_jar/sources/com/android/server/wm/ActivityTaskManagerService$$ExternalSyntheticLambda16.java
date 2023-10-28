package com.android.server.wm;

import android.app.ActivityManagerInternal;
import java.util.function.BiConsumer;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class ActivityTaskManagerService$$ExternalSyntheticLambda16 implements BiConsumer {
    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((ActivityManagerInternal) obj).updateOomLevelsForDisplay(((Integer) obj2).intValue());
    }
}
