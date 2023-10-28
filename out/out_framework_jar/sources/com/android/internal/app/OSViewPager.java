package com.android.internal.app;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.internal.widget.ViewPager;
/* loaded from: classes4.dex */
public class OSViewPager extends ViewPager {
    public OSViewPager(Context context) {
        super(context);
    }

    public OSViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OSViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OSViewPager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.internal.widget.ViewPager, android.view.View
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (View.MeasureSpec.getMode(heightMeasureSpec) != Integer.MIN_VALUE) {
            Log.e("hbhssss", "onMeasure: ================ ");
            return;
        }
        int widthMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        int height = getMeasuredHeight();
        int maxHeight = 0;
        Log.e("hbhssss", "onMeasure: getChildCount() = " + getChildCount());
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec2, View.MeasureSpec.makeMeasureSpec(height, Integer.MIN_VALUE));
            if (maxHeight < child.getMeasuredHeight()) {
                maxHeight = child.getMeasuredHeight();
            }
        }
        if (maxHeight > 0) {
            height = maxHeight;
            Log.e("hbhssss", "onMeasure: height = " + height);
        }
        super.onMeasure(widthMeasureSpec2, View.MeasureSpec.makeMeasureSpec(height, 1073741824));
    }
}
