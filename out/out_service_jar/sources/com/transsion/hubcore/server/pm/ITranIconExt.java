package com.transsion.hubcore.server.pm;

import android.content.Context;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.resolution.ComponentResolver;
import com.android.server.utils.WatchedArrayMap;
import com.transsion.hubcore.server.pm.ITranIconExt;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranIconExt {
    public static final TranClassInfo<ITranIconExt> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.pm.TranIconExtImpl", ITranIconExt.class, new Supplier() { // from class: com.transsion.hubcore.server.pm.ITranIconExt$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranIconExt.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranIconExt {
    }

    static ITranIconExt Instance() {
        return (ITranIconExt) classInfo.getImpl();
    }

    default void setThemeResourceIdForApp(AndroidPackage pkg) {
    }

    default void systemReady(WatchedArrayMap<String, AndroidPackage> packages, boolean isFirstBoot) {
    }

    default void init(Context context, ComponentResolver componentResolver) {
    }

    default void setThemedIcon(ParsedActivity a) {
    }
}
