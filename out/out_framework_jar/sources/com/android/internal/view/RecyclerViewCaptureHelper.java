package com.android.internal.view;

import android.graphics.Rect;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.view.ScrollCaptureViewHelper;
import java.lang.reflect.Method;
import java.util.function.Consumer;
/* loaded from: classes4.dex */
public class RecyclerViewCaptureHelper implements ScrollCaptureViewHelper<ViewGroup> {
    private static final String TAG = "RVCaptureHelper";
    private Rect mInitialGlobalVisiableRect = new Rect();
    private int mOverScrollMode;
    private boolean mScrollBarWasEnabled;
    private int mScrollDelta;

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
        this.mScrollDelta = 0;
        this.mOverScrollMode = view.getOverScrollMode();
        view.setOverScrollMode(2);
        this.mScrollBarWasEnabled = view.isVerticalScrollBarEnabled();
        view.setVerticalScrollBarEnabled(false);
        view.getGlobalVisibleRect(this.mInitialGlobalVisiableRect);
        collapseAppbarIfNeeded(view);
    }

    /* renamed from: onScrollRequested  reason: avoid collision after fix types in other method */
    public void onScrollRequested2(ViewGroup recyclerView, Rect scrollBounds, Rect requestRect, CancellationSignal signal, Consumer<ScrollCaptureViewHelper.ScrollResult> resultConsumer) {
        ScrollCaptureViewHelper.ScrollResult result = new ScrollCaptureViewHelper.ScrollResult();
        result.requestedArea = new Rect(requestRect);
        result.scrollDelta = this.mScrollDelta;
        result.availableArea = new Rect();
        if (!recyclerView.isVisibleToUser() || recyclerView.getChildCount() == 0) {
            Log.w(TAG, "recyclerView is empty or not visible, cannot continue");
            resultConsumer.accept(result);
            return;
        }
        Rect requestedContainerBounds = new Rect(requestRect);
        requestedContainerBounds.offset(0, -this.mScrollDelta);
        requestedContainerBounds.offset(scrollBounds.left, scrollBounds.top);
        View anchor = findChildNearestTarget(recyclerView, requestedContainerBounds);
        if (anchor == null) {
            Log.w(TAG, "Failed to locate anchor view");
            resultConsumer.accept(result);
            return;
        }
        Rect requestedContentBounds = new Rect(requestedContainerBounds);
        recyclerView.offsetRectIntoDescendantCoords(anchor, requestedContentBounds);
        int prevAnchorTop = anchor.getTop();
        Rect input = new Rect(requestedContentBounds);
        int remainingHeight = ((recyclerView.getHeight() - recyclerView.getPaddingTop()) - recyclerView.getPaddingBottom()) - input.height();
        if (remainingHeight > 0) {
            input.inset(0, (-remainingHeight) / 2);
        }
        if (recyclerView.requestChildRectangleOnScreen(anchor, input, true)) {
            int scrolled = prevAnchorTop - anchor.getTop();
            int i = this.mScrollDelta + scrolled;
            this.mScrollDelta = i;
            result.scrollDelta = i;
        } else if (requestChildRectangleOnScreen(recyclerView, anchor, input)) {
            int scrolled2 = prevAnchorTop - anchor.getTop();
            int i2 = this.mScrollDelta + scrolled2;
            this.mScrollDelta = i2;
            result.scrollDelta = i2;
        }
        if (anchor.getParent() == null) {
            Log.w(TAG, "anchor is not a descendant of this view");
            resultConsumer.accept(result);
            return;
        }
        requestedContainerBounds.set(requestedContentBounds);
        recyclerView.offsetDescendantRectToMyCoords(anchor, requestedContainerBounds);
        Rect recyclerLocalVisible = new Rect(scrollBounds);
        recyclerView.getLocalVisibleRect(recyclerLocalVisible);
        if (!requestedContainerBounds.intersect(recyclerLocalVisible)) {
            resultConsumer.accept(result);
            return;
        }
        Rect globalVisiableRect = new Rect();
        recyclerView.getGlobalVisibleRect(globalVisiableRect);
        int dy = this.mInitialGlobalVisiableRect.top - globalVisiableRect.top;
        Rect limitRect = new Rect(requestedContainerBounds.left, scrollBounds.top, requestedContainerBounds.right, scrollBounds.bottom + dy);
        if (!requestedContainerBounds.intersect(limitRect)) {
            resultConsumer.accept(result);
            return;
        }
        Rect available = new Rect(requestedContainerBounds);
        available.offset(-scrollBounds.left, -scrollBounds.top);
        available.offset(0, this.mScrollDelta);
        result.availableArea = available;
        resultConsumer.accept(result);
    }

    static View findChildNearestTarget(ViewGroup parent, Rect targetRect) {
        View selected = null;
        int minCenterDistance = Integer.MAX_VALUE;
        int preferredDistance = (int) (targetRect.height() * 0.25f);
        Rect parentLocalVis = new Rect();
        parent.getLocalVisibleRect(parentLocalVis);
        Rect frame = new Rect();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.getHitRect(frame);
            if (child.getVisibility() == 0) {
                int centerDistance = Math.abs(targetRect.centerY() - frame.centerY());
                if (centerDistance < minCenterDistance) {
                    minCenterDistance = centerDistance;
                    selected = child;
                } else if (frame.intersect(targetRect) && frame.height() > preferredDistance) {
                    selected = child;
                }
            }
        }
        return selected;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.internal.view.ScrollCaptureViewHelper
    public void onPrepareForEnd(ViewGroup view) {
        view.scrollBy(0, -this.mScrollDelta);
        view.setOverScrollMode(this.mOverScrollMode);
        view.setVerticalScrollBarEnabled(this.mScrollBarWasEnabled);
    }

    private void collapseAppbar(ViewGroup view) {
        try {
            Class appBarLayout = Class.forName("com.google.android.material.appbar.AppBarLayout", true, view.getContext().getClassLoader());
            Method appBarExpand = appBarLayout.getDeclaredMethod("setExpanded", Boolean.TYPE);
            appBarExpand.setAccessible(true);
            for (int i = 0; i < view.getChildCount(); i++) {
                View child = view.getChildAt(i);
                if (child.getClass().getName().toLowerCase().contains("appbarlayout")) {
                    appBarExpand.invoke(child, false);
                    return;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "collapseAppbar:" + e);
        }
    }

    private void collapseAppbarIfNeeded(ViewGroup view) {
        if (view != null) {
            View rootView = view.getRootView();
            for (ViewGroup parent = (ViewGroup) view.getParent(); parent != rootView && parent != null; parent = (ViewGroup) parent.getParent()) {
                if (parent.getClass().getName().contains("CoordinatorLayout")) {
                    collapseAppbar(parent);
                    return;
                }
            }
        }
    }

    public boolean requestChildRectangleOnScreen(ViewGroup parent, View child, Rect rect) {
        int dx;
        int parentLeft = parent.getPaddingLeft();
        int parentTop = parent.getPaddingTop();
        int parentRight = parent.getWidth() - parent.getPaddingRight();
        int parentBottom = parent.getHeight() - parent.getPaddingBottom();
        int childLeft = (child.getLeft() + rect.left) - child.getScrollX();
        int childTop = (child.getTop() + rect.top) - child.getScrollY();
        int childRight = rect.width() + childLeft;
        int childBottom = rect.height() + childTop;
        int offScreenLeft = Math.min(0, childLeft - parentLeft);
        int offScreenTop = Math.min(0, childTop - parentTop);
        int offScreenRight = Math.max(0, childRight - parentRight);
        int offScreenBottom = Math.max(0, childBottom - parentBottom);
        if (parent.getLayoutDirection() == 1) {
            dx = offScreenRight != 0 ? offScreenRight : Math.max(offScreenLeft, childRight - parentRight);
        } else {
            dx = offScreenLeft != 0 ? offScreenLeft : Math.min(childLeft - parentLeft, offScreenRight);
        }
        int dy = offScreenTop != 0 ? offScreenTop : Math.min(childTop - parentTop, offScreenBottom);
        if (dx == 0 && dy == 0) {
            return false;
        }
        parent.scrollBy(dx, dy);
        return true;
    }
}
