package android.os;

import android.os.ILooperLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes2.dex */
public interface ILooperLice {
    public static final LiceInfo<ILooperLice> LICE_INFO = new LiceInfo<>("com.transsion.os.LooperLice", ILooperLice.class, new Supplier() { // from class: android.os.ILooperLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ILooperLice.Default();
        }
    });

    /* loaded from: classes2.dex */
    public static class Default implements ILooperLice {
    }

    static ILooperLice instance() {
        return LICE_INFO.getImpl();
    }

    default void printThreadStack(Thread thread, long timeout) {
    }
}
