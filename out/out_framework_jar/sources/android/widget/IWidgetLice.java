package android.widget;

import android.content.ClipData;
import android.view.View;
import android.widget.IWidgetLice;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_FRAMEWORK)
/* loaded from: classes4.dex */
public interface IWidgetLice {
    public static final LiceInfo<IWidgetLice> sLiceInfo = new LiceInfo<>("com.transsion.widget.WidgetLice", IWidgetLice.class, new Supplier() { // from class: android.widget.IWidgetLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IWidgetLice.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements IWidgetLice {
    }

    static IWidgetLice Instance() {
        return sLiceInfo.getImpl();
    }

    default View.DragShadowBuilder createEditorDragShadowBuilder(TextView dragView, String msg) {
        return null;
    }

    default boolean startDragAndDrop(TextView textView, ClipData clipData, Object object, String msg) {
        return false;
    }
}
