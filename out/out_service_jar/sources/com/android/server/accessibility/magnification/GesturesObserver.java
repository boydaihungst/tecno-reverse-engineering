package com.android.server.accessibility.magnification;

import android.view.MotionEvent;
import com.android.server.accessibility.gestures.GestureMatcher;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public final class GesturesObserver implements GestureMatcher.StateChangeListener {
    private final Listener mListener;
    private final List<GestureMatcher> mGestureMatchers = new ArrayList();
    private boolean mObserveStarted = false;
    private boolean mProcessMotionEvent = false;
    private int mCancelledMatcherSize = 0;

    /* loaded from: classes.dex */
    public interface Listener {
        void onGestureCancelled(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        void onGestureCompleted(int i, MotionEvent motionEvent, MotionEvent motionEvent2, int i2);
    }

    public GesturesObserver(Listener listener, GestureMatcher... matchers) {
        this.mListener = listener;
        for (int i = 0; i < matchers.length; i++) {
            matchers[i].setListener(this);
            this.mGestureMatchers.add(matchers[i]);
        }
    }

    public boolean onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (!this.mObserveStarted) {
            if (event.getActionMasked() != 0) {
                this.mListener.onGestureCancelled(event, rawEvent, policyFlags);
                clear();
                return false;
            }
            this.mObserveStarted = true;
        }
        this.mProcessMotionEvent = true;
        for (int i = 0; i < this.mGestureMatchers.size(); i++) {
            GestureMatcher matcher = this.mGestureMatchers.get(i);
            matcher.onMotionEvent(event, rawEvent, policyFlags);
            if (matcher.getState() == 2) {
                clear();
                this.mProcessMotionEvent = false;
                return true;
            }
        }
        this.mProcessMotionEvent = false;
        return false;
    }

    private void clear() {
        for (GestureMatcher matcher : this.mGestureMatchers) {
            matcher.clear();
        }
        this.mCancelledMatcherSize = 0;
        this.mObserveStarted = false;
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher.StateChangeListener
    public void onStateChanged(int gestureId, int state, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (state == 2) {
            this.mListener.onGestureCompleted(gestureId, event, rawEvent, policyFlags);
            if (!this.mProcessMotionEvent) {
                clear();
            }
        } else if (state == 3) {
            int i = this.mCancelledMatcherSize + 1;
            this.mCancelledMatcherSize = i;
            if (i == this.mGestureMatchers.size()) {
                this.mListener.onGestureCancelled(event, rawEvent, policyFlags);
                clear();
            }
        }
    }
}
