package com.android.internal.view;

import android.graphics.Rect;
import android.os.CancellationSignal;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.view.ScrollCaptureViewHelper;
import java.lang.reflect.Method;
import java.util.function.Consumer;
/* loaded from: classes4.dex */
public class SpecialViewCaptureHelper implements ScrollCaptureViewHelper<ViewGroup> {
    private static final String TAG = "SpecialViewCaptureHelper";
    private Method mComputeVerticalScrollExtentMethod;
    private Method mComputeVerticalScrollOffsetMethod;
    private Method mComputeVerticalScrollRangeMethod;
    private int mOriginScrollX;
    private int mOriginScrollY;
    private final Rect mRequestWebViewLocal = new Rect();
    private final Rect mWebViewBounds = new Rect();
    private Class<?> mViewClass = null;

    /* JADX DEBUG: Method arguments types fixed to match base method, original types: [android.view.View, android.graphics.Rect, android.graphics.Rect, android.os.CancellationSignal, java.util.function.Consumer] */
    @Override // com.android.internal.view.ScrollCaptureViewHelper
    public /* bridge */ /* synthetic */ void onScrollRequested(ViewGroup viewGroup, Rect rect, Rect rect2, CancellationSignal cancellationSignal, Consumer consumer) {
        onScrollRequested2(viewGroup, rect, rect2, cancellationSignal, (Consumer<ScrollCaptureViewHelper.ScrollResult>) consumer);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.view.ScrollCaptureViewHelper
    public boolean onAcceptSession(ViewGroup view) {
        return view.isVisibleToUser() && (view.canScrollVertically(-1) || view.canScrollVertically(1));
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.view.ScrollCaptureViewHelper
    public void onPrepareForStart(ViewGroup view, Rect scrollBounds) {
        this.mOriginScrollX = view.getScrollX();
        this.mOriginScrollY = view.getScrollY();
        reflectViewMethod();
    }

    /* renamed from: onScrollRequested  reason: avoid collision after fix types in other method */
    public void onScrollRequested2(ViewGroup view, Rect scrollBounds, Rect requestRect, CancellationSignal signal, Consumer<ScrollCaptureViewHelper.ScrollResult> resultConsumer) {
        int scrollDelta = view.getScrollY() - this.mOriginScrollY;
        ScrollCaptureViewHelper.ScrollResult result = new ScrollCaptureViewHelper.ScrollResult();
        result.requestedArea = new Rect(requestRect);
        result.availableArea = new Rect();
        result.scrollDelta = scrollDelta;
        this.mWebViewBounds.set(0, 0, view.getWidth(), view.getHeight());
        if (!view.isVisibleToUser()) {
            resultConsumer.accept(result);
        }
        boolean canScrollVertically = view.canScrollVertically(1);
        if (!canScrollVertically) {
            resultConsumer.accept(result);
        }
        this.mRequestWebViewLocal.set(requestRect);
        this.mRequestWebViewLocal.offset(0, -scrollDelta);
        Math.min(0, -view.getScrollY());
        int contentHeightPx = (int) (view.getHeight() * view.getScaleY());
        Math.max(0, (contentHeightPx - view.getHeight()) - view.getScrollY());
        int scrollMovement = this.mRequestWebViewLocal.centerY() - this.mWebViewBounds.centerY();
        if (scrollMovement < 0) {
            scrollMovement = 0;
        }
        int scrollMovement2 = Math.abs(Math.min(getScrollY(view), scrollMovement));
        view.scrollBy(this.mOriginScrollX, scrollMovement2);
        int scrollDelta2 = view.getScrollY() - this.mOriginScrollY;
        if (scrollMovement2 != 0 && scrollDelta2 == result.scrollDelta) {
            view.scrollTo(this.mOriginScrollX, view.getScrollY() + scrollMovement2);
            scrollDelta2 = view.getScrollY() - this.mOriginScrollY;
        }
        this.mRequestWebViewLocal.offset(0, -scrollMovement2);
        result.scrollDelta = scrollDelta2;
        result.availableArea = new Rect(this.mRequestWebViewLocal);
        result.availableArea.offset(0, result.scrollDelta);
        resultConsumer.accept(result);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.view.ScrollCaptureViewHelper
    public void onPrepareForEnd(ViewGroup view) {
        view.scrollTo(this.mOriginScrollX, this.mOriginScrollY);
    }

    private void reflectViewMethod() {
        if (this.mViewClass == null) {
            try {
                Class<?> cls = Class.forName("android.view.View");
                this.mViewClass = cls;
                this.mComputeVerticalScrollOffsetMethod = cls.getDeclaredMethod("computeVerticalScrollOffset", new Class[0]);
                this.mComputeVerticalScrollRangeMethod = this.mViewClass.getDeclaredMethod("computeVerticalScrollRange", new Class[0]);
                this.mComputeVerticalScrollExtentMethod = this.mViewClass.getDeclaredMethod("computeVerticalScrollExtent", new Class[0]);
                this.mComputeVerticalScrollOffsetMethod.setAccessible(true);
                this.mComputeVerticalScrollRangeMethod.setAccessible(true);
                this.mComputeVerticalScrollExtentMethod.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getScrollY(View view) {
        try {
            int verticalScrollOffset = ((Integer) this.mComputeVerticalScrollOffsetMethod.invoke(view, new Object[0])).intValue();
            int height = ((Integer) this.mComputeVerticalScrollExtentMethod.invoke(view, new Object[0])).intValue();
            int verticalScrollRangel = ((Integer) this.mComputeVerticalScrollRangeMethod.invoke(view, new Object[0])).intValue();
            int scrolly = (verticalScrollRangel - verticalScrollOffset) - height;
            return scrolly;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
