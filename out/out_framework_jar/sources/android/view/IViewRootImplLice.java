package android.view;

import android.content.Context;
import android.view.IViewRootImplLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes3.dex */
public interface IViewRootImplLice {
    public static final LiceInfo<IViewRootImplLice> sLiceInfo = new LiceInfo<>("com.transsion.view.ViewRootImplLice", IViewRootImplLice.class, new Supplier() { // from class: android.view.IViewRootImplLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IViewRootImplLice.DefaultImpl();
        }
    });

    /* loaded from: classes3.dex */
    public static class DefaultImpl implements IViewRootImplLice {
    }

    static IViewRootImplLice Instance() {
        return sLiceInfo.getImpl();
    }

    default boolean checkMotionEvent(MotionEvent event) {
        return false;
    }

    default void onInit(Context context) {
    }
}
