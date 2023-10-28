package com.transsion.hubcore.iconcrop;

import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.graphics.drawable.Drawable;
import com.transsion.hubcore.iconcrop.ITranNonAdaptiveIconExt;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranNonAdaptiveIconExt {
    public static final TranClassInfo<ITranNonAdaptiveIconExt> classInfo = new TranClassInfo<>("com.transsion.hubcore.iconcrop.TranNonAdaptiveIconExtImpl", ITranNonAdaptiveIconExt.class, new Supplier() { // from class: com.transsion.hubcore.iconcrop.ITranNonAdaptiveIconExt$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranNonAdaptiveIconExt.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranNonAdaptiveIconExt {
    }

    static ITranNonAdaptiveIconExt Instance() {
        return classInfo.getImpl();
    }

    default Drawable getIcon(Context context, PackageItemInfo itemInfo, Drawable icon, int userId) {
        return null;
    }
}
