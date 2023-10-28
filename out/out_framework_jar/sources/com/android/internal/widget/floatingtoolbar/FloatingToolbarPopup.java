package com.android.internal.widget.floatingtoolbar;

import android.content.Context;
import android.graphics.Rect;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;
import com.transsion.hubcore.internal.ITranInternalView;
import java.util.List;
/* loaded from: classes4.dex */
public interface FloatingToolbarPopup {
    void dismiss();

    void hide();

    boolean isHidden();

    boolean isShowing();

    boolean setOutsideTouchable(boolean z, PopupWindow.OnDismissListener onDismissListener);

    void setSuggestedWidth(int i);

    void setWidthChanged(boolean z);

    void show(List<MenuItem> list, MenuItem.OnMenuItemClickListener onMenuItemClickListener, Rect rect);

    static FloatingToolbarPopup createInstance(Context context, View parent) {
        FloatingToolbarPopup popup = ITranInternalView.Instance().createFloatingToolbarPopup(context, parent);
        return popup != null ? popup : new LocalFloatingToolbarPopup(context, parent);
    }
}
