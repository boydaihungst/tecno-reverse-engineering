package android.service.voice;

import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.HotwordDetector;
import java.util.function.BiConsumer;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes3.dex */
public final /* synthetic */ class AbstractHotwordDetector$BinderCallback$$ExternalSyntheticLambda0 implements BiConsumer {
    @Override // java.util.function.BiConsumer
    public final void accept(Object obj, Object obj2) {
        ((HotwordDetector.Callback) obj).onDetected((AlwaysOnHotwordDetector.EventPayload) obj2);
    }
}
