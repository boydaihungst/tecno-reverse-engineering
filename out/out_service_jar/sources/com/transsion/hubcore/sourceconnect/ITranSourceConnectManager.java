package com.transsion.hubcore.sourceconnect;

import android.content.Context;
import android.view.WindowManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranSourceConnectManager {
    public static final TranClassInfo<ITranSourceConnectManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.sourceconnect.TranSourceConnectManagerImpl", ITranSourceConnectManager.class, new Supplier() { // from class: com.transsion.hubcore.sourceconnect.ITranSourceConnectManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranSourceConnectManager.lambda$static$0();
        }
    });

    static /* synthetic */ ITranSourceConnectManager lambda$static$0() {
        return new ITranSourceConnectManager() { // from class: com.transsion.hubcore.sourceconnect.ITranSourceConnectManager.1
        };
    }

    static ITranSourceConnectManager Instance() {
        return (ITranSourceConnectManager) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void hookDisplayRotation(int displayId, int rotation) {
    }

    default void hookDisplayResumedActivityChanged(int displayId, String packageName) {
    }

    default void hookSecureWindowVisible(int displayId, WindowManager.LayoutParams attrs) {
    }

    default void hookInputMethodShown(boolean inputShown) {
    }

    default void hookDisplayChildCount(int displayId, int count) {
    }
}
