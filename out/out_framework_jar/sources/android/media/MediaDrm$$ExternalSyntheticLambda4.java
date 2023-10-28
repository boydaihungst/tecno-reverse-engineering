package android.media;

import android.media.MediaDrm;
import java.util.function.Consumer;
import java.util.function.Function;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class MediaDrm$$ExternalSyntheticLambda4 implements Function {
    public final /* synthetic */ MediaDrm f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.media.MediaDrm.setOnSessionLostStateListener(android.media.MediaDrm$OnSessionLostStateListener, android.os.Handler):void, android.media.MediaDrm.setOnSessionLostStateListener(java.util.concurrent.Executor, android.media.MediaDrm$OnSessionLostStateListener):void] */
    public /* synthetic */ MediaDrm$$ExternalSyntheticLambda4(MediaDrm mediaDrm) {
        this.f$0 = mediaDrm;
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Consumer createOnSessionLostStateListener;
        createOnSessionLostStateListener = this.f$0.createOnSessionLostStateListener((MediaDrm.OnSessionLostStateListener) obj);
        return createOnSessionLostStateListener;
    }
}
