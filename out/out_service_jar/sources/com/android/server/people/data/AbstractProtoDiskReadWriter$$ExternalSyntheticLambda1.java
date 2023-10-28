package com.android.server.people.data;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class AbstractProtoDiskReadWriter$$ExternalSyntheticLambda1 implements Runnable {
    public final /* synthetic */ AbstractProtoDiskReadWriter f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.people.data.AbstractProtoDiskReadWriter.scheduleSave(java.lang.String, T):void, com.android.server.people.data.AbstractProtoDiskReadWriter.triggerScheduledFlushEarly():void] */
    public /* synthetic */ AbstractProtoDiskReadWriter$$ExternalSyntheticLambda1(AbstractProtoDiskReadWriter abstractProtoDiskReadWriter) {
        this.f$0 = abstractProtoDiskReadWriter;
    }

    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.flushScheduledData();
    }
}
