package android.telephony;

import android.telephony.TelephonyManager;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes3.dex */
public final /* synthetic */ class TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda1 implements Supplier {
    public final /* synthetic */ WeakReference f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.telephony.TelephonyRegistryManager.CarrierPrivilegesCallbackWrapper.lambda$onCarrierPrivilegesChanged$1$android-telephony-TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper(java.util.Set, java.util.Set):void, android.telephony.TelephonyRegistryManager.CarrierPrivilegesCallbackWrapper.lambda$onCarrierServiceChanged$3$android-telephony-TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper(java.lang.String, int):void] */
    public /* synthetic */ TelephonyRegistryManager$CarrierPrivilegesCallbackWrapper$$ExternalSyntheticLambda1(WeakReference weakReference) {
        this.f$0 = weakReference;
    }

    @Override // java.util.function.Supplier
    public final Object get() {
        return (TelephonyManager.CarrierPrivilegesCallback) this.f$0.get();
    }
}
