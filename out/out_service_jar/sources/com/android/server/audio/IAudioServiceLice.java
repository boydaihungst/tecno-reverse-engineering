package com.android.server.audio;

import android.content.Context;
import com.android.server.audio.IAudioServiceLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface IAudioServiceLice {
    public static final LiceInfo<IAudioServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.audio.AudioServiceLice", IAudioServiceLice.class, new Supplier() { // from class: com.android.server.audio.IAudioServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IAudioServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IAudioServiceLice {
    }

    static IAudioServiceLice Instance() {
        return (IAudioServiceLice) sLiceInfo.getImpl();
    }

    default void notifyModeChange(int mode, String callingPackage) {
    }

    default boolean isTheftAlertRinging(Context context) {
        return false;
    }
}
