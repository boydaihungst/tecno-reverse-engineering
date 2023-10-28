package android.view.accessibility;

import android.view.accessibility.IAccessibilityLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes3.dex */
public interface IAccessibilityLice {
    public static final LiceInfo<IAccessibilityLice> sLiceInfo = new LiceInfo<>("com.transsion.view.accessibility.AccessibilityLice", IAccessibilityLice.class, new Supplier() { // from class: android.view.accessibility.IAccessibilityLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IAccessibilityLice.DefaultImpl();
        }
    });

    /* loaded from: classes3.dex */
    public static class DefaultImpl implements IAccessibilityLice {
    }

    static IAccessibilityLice Instance() {
        return sLiceInfo.getImpl();
    }

    default String osHookForCtsTest(CharSequence pkg, String viewIdResourceName) {
        return viewIdResourceName;
    }
}
