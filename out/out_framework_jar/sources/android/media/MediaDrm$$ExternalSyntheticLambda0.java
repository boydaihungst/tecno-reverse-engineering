package android.media;

import android.media.MediaDrm;
import java.util.function.Function;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class MediaDrm$$ExternalSyntheticLambda0 implements Function {
    public final /* synthetic */ MediaDrm f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.media.MediaDrm.setOnEventListener(android.media.MediaDrm$OnEventListener, android.os.Handler):void, android.media.MediaDrm.setOnEventListener(java.util.concurrent.Executor, android.media.MediaDrm$OnEventListener):void] */
    public /* synthetic */ MediaDrm$$ExternalSyntheticLambda0(MediaDrm mediaDrm) {
        this.f$0 = mediaDrm;
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return MediaDrm.$r8$lambda$6kH9US9tXifhPviAfmxnnSizUU0(this.f$0, (MediaDrm.OnEventListener) obj);
    }
}
