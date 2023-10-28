package android.view;

import android.content.ClipData;
import android.content.Context;
import android.view.IViewLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes3.dex */
public interface IViewLice {
    public static final LiceInfo<IViewLice> sLiceInfo = new LiceInfo<>("com.transsion.view.ViewLice", IViewLice.class, new Supplier() { // from class: android.view.IViewLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IViewLice.DefaultImpl();
        }
    });

    /* loaded from: classes3.dex */
    public static class DefaultImpl implements IViewLice {
    }

    /* loaded from: classes3.dex */
    public interface IUpdateClipDataCallback {
        boolean updateClipData(ClipData clipData);
    }

    static IViewLice Instance() {
        return sLiceInfo.getImpl();
    }

    default void initViewGroup(Context context) {
    }

    default void computeScroll(ViewGroup viewgroup) {
    }

    default void dispatchTouch(ViewGroup viewgroup, MotionEvent ev) {
    }

    default void checkInterruptScreenShot(Context mContext, ViewGroup viewgroup) {
    }

    default void setStartInMultiWindow(int viewId, String pkg) {
    }

    default void collapsePanels(int viewId) {
    }

    default boolean isSupportedUpdateClipData(int flags) {
        return false;
    }

    default boolean updateClipData(int flags, ClipData data, IUpdateClipDataCallback callback) {
        return false;
    }

    default void callDragEventHandler(boolean enable, Context context, String packageName, DragEvent event) {
    }

    default boolean interceptedDispatchEvent(MotionEvent ev, boolean defaultIntercepted) {
        return defaultIntercepted;
    }
}
