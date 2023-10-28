package android.app;

import android.app.IActivityOptionsLice;
import android.os.Bundle;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes.dex */
public interface IActivityOptionsLice {
    public static final LiceInfo<IActivityOptionsLice> LICE_INFO = new LiceInfo<>("com.transsion.app.ActivityOptionsLice", IActivityOptionsLice.class, new Supplier() { // from class: android.app.IActivityOptionsLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IActivityOptionsLice.Default();
        }
    });

    /* loaded from: classes.dex */
    public static class Default implements IActivityOptionsLice {
    }

    static IActivityOptionsLice instance() {
        return LICE_INFO.getImpl();
    }

    default void onConstruct(ActivityOptions activityOptions, Bundle options) {
    }
}
