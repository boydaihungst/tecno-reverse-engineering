package com.android.server.accessibility.gestures;

import android.accessibilityservice.AccessibilityGestureEvent;
import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import com.android.server.accessibility.gestures.GestureMatcher;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GestureManifold implements GestureMatcher.StateChangeListener {
    private static final String LOG_TAG = "GestureManifold";
    private final Context mContext;
    private List<MotionEvent> mEvents;
    private final List<GestureMatcher> mGestures;
    private final Handler mHandler;
    private Listener mListener;
    private final List<GestureMatcher> mMultiFingerGestures;
    boolean mMultiFingerGesturesEnabled;
    private boolean mSendMotionEventsEnabled;
    private boolean mServiceHandlesDoubleTap;
    private TouchState mState;
    private boolean mTwoFingerPassthroughEnabled;
    private final List<GestureMatcher> mTwoFingerSwipes;

    /* loaded from: classes.dex */
    public interface Listener {
        boolean onDoubleTap(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        void onDoubleTapAndHold(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        boolean onGestureCancelled(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        boolean onGestureCompleted(AccessibilityGestureEvent accessibilityGestureEvent);

        boolean onGestureStarted();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GestureManifold(Context context, Listener listener, TouchState state, Handler handler) {
        ArrayList arrayList = new ArrayList();
        this.mGestures = arrayList;
        this.mServiceHandlesDoubleTap = false;
        this.mSendMotionEventsEnabled = false;
        ArrayList arrayList2 = new ArrayList();
        this.mMultiFingerGestures = arrayList2;
        ArrayList arrayList3 = new ArrayList();
        this.mTwoFingerSwipes = arrayList3;
        this.mEvents = new ArrayList();
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mState = state;
        this.mMultiFingerGesturesEnabled = false;
        this.mTwoFingerPassthroughEnabled = false;
        arrayList.add(new MultiTap(context, 2, 17, this));
        arrayList.add(new MultiTapAndHold(context, 2, 18, this));
        arrayList.add(new SecondFingerMultiTap(context, 2, 17, this));
        arrayList.add(new Swipe(context, 1, 4, this));
        arrayList.add(new Swipe(context, 0, 3, this));
        arrayList.add(new Swipe(context, 2, 1, this));
        arrayList.add(new Swipe(context, 3, 2, this));
        arrayList.add(new Swipe(context, 0, 1, 5, this));
        arrayList.add(new Swipe(context, 0, 2, 9, this));
        arrayList.add(new Swipe(context, 0, 3, 10, this));
        arrayList.add(new Swipe(context, 1, 2, 11, this));
        arrayList.add(new Swipe(context, 1, 3, 12, this));
        arrayList.add(new Swipe(context, 1, 0, 6, this));
        arrayList.add(new Swipe(context, 3, 2, 8, this));
        arrayList.add(new Swipe(context, 3, 0, 15, this));
        arrayList.add(new Swipe(context, 3, 1, 16, this));
        arrayList.add(new Swipe(context, 2, 3, 7, this));
        arrayList.add(new Swipe(context, 2, 0, 13, this));
        arrayList.add(new Swipe(context, 2, 1, 14, this));
        arrayList2.add(new MultiFingerMultiTap(context, 2, 1, 19, this));
        arrayList2.add(new MultiFingerMultiTap(context, 2, 2, 20, this));
        arrayList2.add(new MultiFingerMultiTapAndHold(context, 2, 2, 40, this));
        arrayList2.add(new MultiFingerMultiTap(context, 2, 3, 21, this));
        arrayList2.add(new MultiFingerMultiTapAndHold(context, 2, 3, 43, this));
        arrayList2.add(new MultiFingerMultiTap(context, 3, 1, 22, this));
        arrayList2.add(new MultiFingerMultiTap(context, 3, 2, 23, this));
        arrayList2.add(new MultiFingerMultiTapAndHold(context, 3, 1, 44, this));
        arrayList2.add(new MultiFingerMultiTapAndHold(context, 3, 2, 41, this));
        arrayList2.add(new MultiFingerMultiTap(context, 3, 3, 24, this));
        arrayList2.add(new MultiFingerMultiTapAndHold(context, 3, 3, 45, this));
        arrayList2.add(new MultiFingerMultiTap(context, 3, 3, 24, this));
        arrayList2.add(new MultiFingerMultiTap(context, 4, 1, 37, this));
        arrayList2.add(new MultiFingerMultiTap(context, 4, 2, 38, this));
        arrayList2.add(new MultiFingerMultiTapAndHold(context, 4, 2, 42, this));
        arrayList2.add(new MultiFingerMultiTap(context, 4, 3, 39, this));
        arrayList3.add(new MultiFingerSwipe(context, 2, 3, 26, this));
        arrayList3.add(new MultiFingerSwipe(context, 2, 0, 27, this));
        arrayList3.add(new MultiFingerSwipe(context, 2, 1, 28, this));
        arrayList3.add(new MultiFingerSwipe(context, 2, 2, 25, this));
        arrayList2.addAll(arrayList3);
        arrayList2.add(new MultiFingerSwipe(context, 3, 3, 30, this));
        arrayList2.add(new MultiFingerSwipe(context, 3, 0, 31, this));
        arrayList2.add(new MultiFingerSwipe(context, 3, 1, 32, this));
        arrayList2.add(new MultiFingerSwipe(context, 3, 2, 29, this));
        arrayList2.add(new MultiFingerSwipe(context, 4, 3, 34, this));
        arrayList2.add(new MultiFingerSwipe(context, 4, 0, 35, this));
        arrayList2.add(new MultiFingerSwipe(context, 4, 1, 36, this));
        arrayList2.add(new MultiFingerSwipe(context, 4, 2, 33, this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mState.isClear()) {
            if (event.getActionMasked() != 0) {
                return false;
            }
            clear();
        }
        if (this.mSendMotionEventsEnabled) {
            this.mEvents.add(MotionEvent.obtainNoHistory(rawEvent));
        }
        for (GestureMatcher matcher : this.mGestures) {
            if (matcher.getState() != 3) {
                matcher.onMotionEvent(event, rawEvent, policyFlags);
                if (matcher.getState() == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clear() {
        for (GestureMatcher matcher : this.mGestures) {
            matcher.clear();
        }
        if (this.mEvents != null) {
            while (this.mEvents.size() > 0) {
                this.mEvents.remove(0).recycle();
            }
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher.StateChangeListener
    public void onStateChanged(int gestureId, int state, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (state == 1 && !this.mState.isGestureDetecting()) {
            if (gestureId == 17 || gestureId == 18) {
                if (this.mServiceHandlesDoubleTap) {
                    this.mListener.onGestureStarted();
                    return;
                }
                return;
            }
            this.mListener.onGestureStarted();
        } else if (state == 2) {
            onGestureCompleted(gestureId, event, rawEvent, policyFlags);
        } else if (state == 3 && this.mState.isGestureDetecting()) {
            for (GestureMatcher matcher : this.mGestures) {
                if (matcher.getState() == 1) {
                    return;
                }
            }
            this.mListener.onGestureCancelled(event, rawEvent, policyFlags);
        }
    }

    private void onGestureCompleted(int gestureId, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        switch (gestureId) {
            case 17:
                if (this.mServiceHandlesDoubleTap) {
                    AccessibilityGestureEvent gestureEvent = new AccessibilityGestureEvent(gestureId, event.getDisplayId(), this.mEvents);
                    this.mListener.onGestureCompleted(gestureEvent);
                    break;
                } else {
                    this.mListener.onDoubleTap(event, rawEvent, policyFlags);
                    break;
                }
            case 18:
                if (this.mServiceHandlesDoubleTap) {
                    AccessibilityGestureEvent gestureEvent2 = new AccessibilityGestureEvent(gestureId, event.getDisplayId(), this.mEvents);
                    this.mListener.onGestureCompleted(gestureEvent2);
                    break;
                } else {
                    this.mListener.onDoubleTapAndHold(event, rawEvent, policyFlags);
                    break;
                }
            default:
                AccessibilityGestureEvent gestureEvent3 = new AccessibilityGestureEvent(gestureId, event.getDisplayId(), this.mEvents);
                this.mListener.onGestureCompleted(gestureEvent3);
                break;
        }
        clear();
    }

    public boolean isMultiFingerGesturesEnabled() {
        return this.mMultiFingerGesturesEnabled;
    }

    public void setMultiFingerGesturesEnabled(boolean mode) {
        if (this.mMultiFingerGesturesEnabled != mode) {
            this.mMultiFingerGesturesEnabled = mode;
            if (mode) {
                this.mGestures.addAll(this.mMultiFingerGestures);
            } else {
                this.mGestures.removeAll(this.mMultiFingerGestures);
            }
        }
    }

    public boolean isTwoFingerPassthroughEnabled() {
        return this.mTwoFingerPassthroughEnabled;
    }

    public void setTwoFingerPassthroughEnabled(boolean mode) {
        if (this.mTwoFingerPassthroughEnabled != mode) {
            this.mTwoFingerPassthroughEnabled = mode;
            if (!mode) {
                this.mMultiFingerGestures.addAll(this.mTwoFingerSwipes);
                if (this.mMultiFingerGesturesEnabled) {
                    this.mGestures.addAll(this.mTwoFingerSwipes);
                    return;
                }
                return;
            }
            this.mMultiFingerGestures.removeAll(this.mTwoFingerSwipes);
            this.mGestures.removeAll(this.mTwoFingerSwipes);
        }
    }

    public void setServiceHandlesDoubleTap(boolean mode) {
        this.mServiceHandlesDoubleTap = mode;
    }

    public boolean isServiceHandlesDoubleTapEnabled() {
        return this.mServiceHandlesDoubleTap;
    }

    public void setSendMotionEventsEnabled(boolean mode) {
        this.mSendMotionEventsEnabled = mode;
        if (!mode) {
            while (this.mEvents.size() > 0) {
                this.mEvents.remove(0).recycle();
            }
        }
    }

    public boolean isSendMotionEventsEnabled() {
        return this.mSendMotionEventsEnabled;
    }

    public List<MotionEvent> getMotionEvents() {
        return this.mEvents;
    }
}
