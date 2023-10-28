package com.transsion.hubcore.telephony;

import android.content.Context;
import android.graphics.Bitmap;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranSubscription {
    public static final TranClassInfo<ITranSubscription> classInfo = new TranClassInfo<>("com.transsion.hubcore.telephony.TranSubscriptionImpl", ITranSubscription.class, new Supplier() { // from class: com.transsion.hubcore.telephony.ITranSubscription$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranSubscription.lambda$static$0();
        }
    });

    static /* synthetic */ ITranSubscription lambda$static$0() {
        return new ITranSubscription() { // from class: com.transsion.hubcore.telephony.ITranSubscription.1
        };
    }

    static ITranSubscription Instance() {
        return classInfo.getImpl();
    }

    default Bitmap createIconBitmap(Context context, int slot) {
        return null;
    }
}
