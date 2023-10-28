package android.media;

import android.media.MediaDrm;
import java.util.function.Function;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class MediaDrm$$ExternalSyntheticLambda1 implements Function {
    public final /* synthetic */ MediaDrm f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.media.MediaDrm.setOnExpirationUpdateListener(android.media.MediaDrm$OnExpirationUpdateListener, android.os.Handler):void, android.media.MediaDrm.setOnExpirationUpdateListener(java.util.concurrent.Executor, android.media.MediaDrm$OnExpirationUpdateListener):void] */
    public /* synthetic */ MediaDrm$$ExternalSyntheticLambda1(MediaDrm mediaDrm) {
        this.f$0 = mediaDrm;
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return this.f$0.createOnExpirationUpdateListener((MediaDrm.OnExpirationUpdateListener) obj);
    }
}
