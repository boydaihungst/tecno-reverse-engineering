package com.android.server.wm;

import java.util.ArrayList;
import java.util.function.Consumer;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class Task$$ExternalSyntheticLambda46 implements Consumer {
    public final /* synthetic */ ArrayList f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.wm.Task.getDumpActivitiesLocked(java.lang.String, int):java.util.ArrayList<com.android.server.wm.ActivityRecord>, com.android.server.wm.TaskFragment.remove(boolean, java.lang.String):void] */
    public /* synthetic */ Task$$ExternalSyntheticLambda46(ArrayList arrayList) {
        this.f$0 = arrayList;
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        this.f$0.add((ActivityRecord) obj);
    }
}
