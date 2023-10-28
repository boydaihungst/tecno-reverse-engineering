package com.android.server;

import android.content.Context;
import android.view.MotionEvent;
import com.android.server.IInputLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface IInputLice {
    public static final LiceInfo<IInputLice> sLiceInfo = new LiceInfo<>("com.transsion.server.InputLice", IInputLice.class, new Supplier() { // from class: com.android.server.IInputLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IInputLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IInputLice {
    }

    static IInputLice Instance() {
        return (IInputLice) sLiceInfo.getImpl();
    }

    default void init(Context context) {
    }

    default boolean checkMotionEvent(MotionEvent event) {
        return false;
    }

    default int checkEdgeMotionEvent(MotionEvent event) {
        return 0;
    }
}
