package com.transsion.hubcore.sru;

import android.content.Context;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranSruManager {
    public static final TranClassInfo<ITranSruManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.sru.TranSruManagerImpl", ITranSruManager.class, new Supplier() { // from class: com.transsion.hubcore.sru.ITranSruManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranSruManager.lambda$static$0();
        }
    });

    static /* synthetic */ ITranSruManager lambda$static$0() {
        return new ITranSruManager() { // from class: com.transsion.hubcore.sru.ITranSruManager.1
        };
    }

    static ITranSruManager Instance() {
        return (ITranSruManager) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void hookFinishBoot() {
    }

    default void hookInputMethodShown(boolean inputShown) {
    }

    default void setWhiteList(List<String> list) {
    }

    default List<String> getWhiteList() {
        return null;
    }

    default boolean isInWhiteList(String packageName) {
        return false;
    }

    default boolean isValidCallingUid(String callingPackageName) {
        return false;
    }

    default void sendShutdownOrBootEvent(int type, String reason) {
    }
}
