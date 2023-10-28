package android.service.voice;

import android.service.voice.HotwordDetector;
import java.util.function.Consumer;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes3.dex */
public final /* synthetic */ class AbstractHotwordDetector$BinderCallback$$ExternalSyntheticLambda1 implements Consumer {
    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((HotwordDetector.Callback) obj).onError();
    }
}
