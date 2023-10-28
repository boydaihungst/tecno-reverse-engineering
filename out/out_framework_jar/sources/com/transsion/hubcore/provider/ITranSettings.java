package com.transsion.hubcore.provider;

import android.app.Application;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranSettings {
    public static final TranClassInfo<ITranSettings> classInfo = new TranClassInfo<>("com.transsion.hubcore.provider.TranSettingsImpl", ITranSettings.class, new Supplier() { // from class: com.transsion.hubcore.provider.ITranSettings$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranSettings.lambda$static$0();
        }
    });

    static /* synthetic */ ITranSettings lambda$static$0() {
        return new ITranSettings() { // from class: com.transsion.hubcore.provider.ITranSettings.1
        };
    }

    static ITranSettings instance() {
        return classInfo.getImpl();
    }

    default boolean isRestrictPutStringForUser(String name, String value, int uid, Application application) {
        return false;
    }
}
