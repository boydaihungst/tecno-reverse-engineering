package com.android.internal.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.ScrollCaptureCallback;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ListView;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes4.dex */
public class ScrollCaptureInternal {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_VERBOSE = false;
    private static final int DOWN = 1;
    private static final String TAG = "ScrollCaptureInternal";
    public static final int TYPE_FIXED = 0;
    private static final int TYPE_OPAQUE = 3;
    public static final int TYPE_RECYCLING = 2;
    public static final int TYPE_SCROLLING = 1;
    private static final int UP = -1;
    private static final List<String> sBlackList = Arrays.asList("com.tencent.xweb");

    private static int detectScrollingType(View view) {
        if (view.canScrollVertically(1)) {
            if (view instanceof ViewGroup) {
                if (((ViewGroup) view).getChildCount() > 1) {
                    return 2;
                }
                if (((ViewGroup) view).getChildCount() < 1) {
                    return 3;
                }
                if (view.getScrollY() != 0) {
                    return 1;
                }
                Log.v(TAG, "hint: scrollY == 0");
                if (view.canScrollVertically(-1)) {
                    return 2;
                }
                view.scrollTo(view.getScrollX(), 1);
                if (view.getScrollY() == 1) {
                    view.scrollTo(view.getScrollX(), 0);
                    return 1;
                }
                return 2;
            }
            return 3;
        }
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public ScrollCaptureCallback requestCallback(View view, Rect localVisibleRect, Point positionInWindow) {
        if (isMatchBlacklist(view)) {
            Log.i(TAG, "ignore this view:" + view.getClass().getName());
            return null;
        }
        int i = detectScrollingType(view);
        switch (i) {
            case 1:
                return new ScrollCaptureViewSupport((ViewGroup) view, new ScrollViewCaptureHelper());
            case 2:
                if (view instanceof ListView) {
                    return new ScrollCaptureViewSupport((ListView) view, new ListViewCaptureHelper());
                }
                return new ScrollCaptureViewSupport((ViewGroup) view, new RecyclerViewCaptureHelper());
            case 3:
                if (view instanceof WebView) {
                    Log.d(TAG, "scroll capture: Using WebView support");
                    return new ScrollCaptureViewSupport((WebView) view, new WebViewCaptureHelper());
                } else if (view instanceof ViewGroup) {
                    return new ScrollCaptureViewSupport((ViewGroup) view, new SpecialViewCaptureHelper());
                }
                break;
        }
        return null;
    }

    private static String formatIntToHexString(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    static String resolveId(Context context, int id) {
        Resources resources = context.getResources();
        if (id >= 0) {
            try {
                String fieldValue = resources.getResourceTypeName(id) + '/' + resources.getResourceEntryName(id);
                return fieldValue;
            } catch (Resources.NotFoundException e) {
                String fieldValue2 = "id/" + formatIntToHexString(id);
                return fieldValue2;
            }
        }
        return "NO_ID";
    }

    private boolean isMatchBlacklist(View view) {
        for (String item : sBlackList) {
            if (view.getClass().getName().startsWith(item)) {
                return true;
            }
        }
        return false;
    }
}
