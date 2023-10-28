package com.mediatek.boostfwk.info;

import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import com.mediatek.boostfwk.utils.LogUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
/* loaded from: classes.dex */
public final class ScrollState {
    private static HashMap<Object, State> scrollingState = new HashMap<>();
    private static ArrayList<ScrollStateListener> mScrollStateListeners = new ArrayList<>(4);
    private static ArrayList<RefreshRateChangedListener> mRefreshRateListeners = new ArrayList<>(8);
    private static DisplayManager.DisplayListener mDisplayListener = null;
    private static float mRefreshRate = -1.0f;
    private static Object lock = new Object();
    private static boolean isScrolling = false;
    private static boolean mIsScrolling = false;
    private static boolean mIsFling = false;

    /* loaded from: classes.dex */
    public interface RefreshRateChangedListener {
        void onDisplayRefreshRateChanged(int i, float f, float f2);
    }

    /* loaded from: classes.dex */
    public interface ScrollStateListener {
        void onScroll(boolean z);
    }

    /* loaded from: classes.dex */
    public static class State {
        boolean scrolling = false;
        int velocityX = -1;
        int velocityY = -1;
    }

    public static void updateState(Object scroller, boolean scrolling) {
        if (scroller == null) {
            return;
        }
        State state = scrollingState.get(scroller);
        if (state == null) {
            State state2 = new State();
            state2.scrolling = scrolling;
            scrollingState.put(scroller, state2);
        } else {
            state.scrolling = scrolling;
        }
        if (scrolling) {
            isScrolling = true;
        } else {
            isScrolling = computeScrollingState();
        }
        Iterator<ScrollStateListener> it = mScrollStateListeners.iterator();
        while (it.hasNext()) {
            ScrollStateListener scrollStateListener = it.next();
            scrollStateListener.onScroll(isScrolling);
        }
    }

    public static void removeState(Object scroller) {
        if (scroller == null) {
            return;
        }
        scrollingState.remove(scroller);
    }

    public static void clearAll() {
        scrollingState.clear();
    }

    private static boolean computeScrollingState() {
        Set<Object> scrollers = scrollingState.keySet();
        for (Object scroller : scrollers) {
            State state = scrollingState.get(scroller);
            if (state.scrolling) {
                return true;
            }
        }
        return false;
    }

    public static boolean isScrolling() {
        return isScrolling;
    }

    public static boolean isScrollerScrolling(Object scroller) {
        State state;
        return (scroller == null || (state = scrollingState.get(scroller)) == null || !state.scrolling) ? false : true;
    }

    public static void registerScrollStateListener(ScrollStateListener listener) {
        if (listener == null) {
            return;
        }
        mScrollStateListeners.add(listener);
    }

    public static void unregisterScrollStateListener(ScrollStateListener listener) {
        if (listener == null) {
            return;
        }
        mScrollStateListeners.remove(listener);
    }

    public static boolean onScrolling() {
        return mIsScrolling;
    }

    public static void setScrolling(boolean scroll, String msg) {
        mIsScrolling = scroll;
        Iterator<ScrollStateListener> it = mScrollStateListeners.iterator();
        while (it.hasNext()) {
            ScrollStateListener scrollStateListener = it.next();
            scrollStateListener.onScroll(mIsScrolling);
        }
        LogUtil.trace("scroll state changed to " + scroll + " because:" + msg);
    }

    public static boolean getFling() {
        return mIsFling;
    }

    public static void setFling(boolean fling) {
        mIsFling = fling;
    }

    public static void registerRefreshRateChangedListener(RefreshRateChangedListener listener) {
        if (listener == null) {
            return;
        }
        mRefreshRateListeners.add(listener);
    }

    public static void unregisterRefreshRateChangedListener(RefreshRateChangedListener listener) {
        if (listener == null) {
            return;
        }
        mRefreshRateListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized void notifyRefreshRateChangedIfNeeded() {
        synchronized (ScrollState.class) {
            float refreshRate = DisplayManagerGlobal.getInstance().getDisplayInfo(0).getRefreshRate();
            if (refreshRate != mRefreshRate) {
                mRefreshRate = refreshRate;
                float frameIntervalNanos = 1.0E9f / refreshRate;
                Iterator<RefreshRateChangedListener> it = mRefreshRateListeners.iterator();
                while (it.hasNext()) {
                    RefreshRateChangedListener listener = it.next();
                    listener.onDisplayRefreshRateChanged(0, refreshRate, frameIntervalNanos);
                }
            }
        }
    }

    private static void registerDisplyListenerIfNeeded() {
        synchronized (lock) {
            if (mDisplayListener != null) {
                return;
            }
            mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.mediatek.boostfwk.info.ScrollState.1
                @Override // android.hardware.display.DisplayManager.DisplayListener
                public void onDisplayAdded(int displayId) {
                }

                @Override // android.hardware.display.DisplayManager.DisplayListener
                public void onDisplayRemoved(int displayId) {
                }

                @Override // android.hardware.display.DisplayManager.DisplayListener
                public void onDisplayChanged(int displayId) {
                    if (displayId == 0) {
                        ScrollState.notifyRefreshRateChangedIfNeeded();
                    }
                }
            };
            DisplayManagerGlobal.getInstance().registerDisplayListener(mDisplayListener, new Handler(), 4L);
        }
    }

    public static float getRefreshRate() {
        if (mRefreshRate == -1.0f) {
            float refreshRate = DisplayManagerGlobal.getInstance().getDisplayInfo(0).getRefreshRate();
            mRefreshRate = refreshRate;
            registerDisplyListenerIfNeeded();
        }
        float refreshRate2 = mRefreshRate;
        return refreshRate2;
    }
}
