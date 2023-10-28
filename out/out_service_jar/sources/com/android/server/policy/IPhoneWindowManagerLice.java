package com.android.server.policy;

import android.content.Context;
import android.util.Pair;
import android.view.KeyEvent;
import com.android.server.policy.IPhoneWindowManagerLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IPhoneWindowManagerLice {
    public static final LiceInfo<IPhoneWindowManagerLice> sLiceInfo = new LiceInfo<>("com.transsion.server.policy.PhoneWindowManagerLice", IPhoneWindowManagerLice.class, new Supplier() { // from class: com.android.server.policy.IPhoneWindowManagerLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IPhoneWindowManagerLice.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements IPhoneWindowManagerLice {
    }

    static IPhoneWindowManagerLice Instance() {
        return (IPhoneWindowManagerLice) sLiceInfo.getImpl();
    }

    default void onStartedGoingToSleep() {
    }

    default void onFinishedGoingToSleep() {
    }

    default void onFinishedWakingUp() {
    }

    default void onScreenTurnedOff() {
    }

    default void onScreenTurningOn() {
    }

    default void onScreenTurnedOn() {
    }

    default void onSystemBooted() {
    }

    default void onSystemBootedEnd() {
    }

    default void onInterceptScrollShot(KeyEvent event) {
    }

    default Pair<Boolean, Integer> onInterceptKeyBeforeQueueing(KeyEvent event) {
        return new Pair<>(false, null);
    }

    default void onInit(Context context) {
    }

    default Pair<Boolean, Long> onInterceptKeyBeforeDispatchingInner(KeyEvent event, int policyFlags) {
        return new Pair<>(false, null);
    }
}
