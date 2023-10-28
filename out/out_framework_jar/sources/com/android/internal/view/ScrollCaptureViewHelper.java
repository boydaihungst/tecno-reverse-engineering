package com.android.internal.view;

import android.graphics.Rect;
import android.os.CancellationSignal;
import android.view.View;
import android.view.ViewGroup;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes4.dex */
public interface ScrollCaptureViewHelper<V extends View> {
    public static final int DOWN = 1;
    public static final int UP = -1;
    public static final List<String> sIgnorePaddingViewsList = Arrays.asList("com.transsion.xlauncher.library.springview.SpringRecyclerView", "com.transsion.widgetslib.view.damping.OSRefreshRecyclerView");

    boolean onAcceptSession(V v);

    void onPrepareForEnd(V v);

    void onPrepareForStart(V v, Rect rect);

    void onScrollRequested(V v, Rect rect, Rect rect2, CancellationSignal cancellationSignal, Consumer<ScrollResult> consumer);

    /* loaded from: classes4.dex */
    public static class ScrollResult {
        public Rect availableArea;
        public Rect requestedArea;
        public int scrollDelta;

        public String toString() {
            return "ScrollResult{requestedArea=" + this.requestedArea + ", availableArea=" + this.availableArea + ", scrollDelta=" + this.scrollDelta + '}';
        }
    }

    default Rect onComputeScrollBounds(V view) {
        Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
        if (sIgnorePaddingViewsList.contains(view.getClass().getName())) {
            return bounds;
        }
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getClipToPadding()) {
            bounds.inset(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
        }
        return bounds;
    }
}
