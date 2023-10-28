package com.android.server.accessibility.magnification;

import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.server.accessibility.gestures.GestureMatcher;
import com.android.server.accessibility.magnification.GesturesObserver;
import java.util.LinkedList;
import java.util.List;
/* loaded from: classes.dex */
class MagnificationGesturesObserver implements GesturesObserver.Listener {
    private final Callback mCallback;
    private List<MotionEventInfo> mDelayedEventQueue;
    private final GesturesObserver mGesturesObserver;
    private long mLastDownEventTime = 0;
    private MotionEvent mLastEvent;
    private static final String LOG_TAG = "MagnificationGesturesObserver";
    private static final boolean DBG = Log.isLoggable(LOG_TAG, 3);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callback {
        void onGestureCancelled(long j, List<MotionEventInfo> list, MotionEvent motionEvent);

        void onGestureCompleted(int i, long j, List<MotionEventInfo> list, MotionEvent motionEvent);

        boolean shouldStopDetection(MotionEvent motionEvent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MagnificationGesturesObserver(Callback callback, GestureMatcher... matchers) {
        this.mGesturesObserver = new GesturesObserver(this, matchers);
        this.mCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (DBG) {
            Slog.d(LOG_TAG, "DetectGesture: event = " + event);
        }
        cacheDelayedMotionEvent(event, rawEvent, policyFlags);
        if (this.mCallback.shouldStopDetection(event)) {
            notifyDetectionCancel();
            return false;
        }
        if (event.getActionMasked() == 0) {
            this.mLastDownEventTime = event.getDownTime();
        }
        return this.mGesturesObserver.onMotionEvent(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.magnification.GesturesObserver.Listener
    public void onGestureCompleted(int gestureId, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (DBG) {
            Slog.d(LOG_TAG, "onGestureCompleted: " + MagnificationGestureMatcher.gestureIdToString(gestureId) + " event = " + event);
        }
        List<MotionEventInfo> delayEventQueue = this.mDelayedEventQueue;
        this.mDelayedEventQueue = null;
        this.mCallback.onGestureCompleted(gestureId, this.mLastDownEventTime, delayEventQueue, event);
        clear();
    }

    @Override // com.android.server.accessibility.magnification.GesturesObserver.Listener
    public void onGestureCancelled(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (DBG) {
            Slog.d(LOG_TAG, "onGestureCancelled:  event = " + event);
        }
        notifyDetectionCancel();
    }

    private void notifyDetectionCancel() {
        List<MotionEventInfo> delayEventQueue = this.mDelayedEventQueue;
        this.mDelayedEventQueue = null;
        this.mCallback.onGestureCancelled(this.mLastDownEventTime, delayEventQueue, this.mLastEvent);
        clear();
    }

    private void clear() {
        if (DBG) {
            Slog.d(LOG_TAG, "clear:" + this.mDelayedEventQueue);
        }
        recycleLastEvent();
        this.mLastDownEventTime = 0L;
        List<MotionEventInfo> list = this.mDelayedEventQueue;
        if (list != null) {
            for (MotionEventInfo eventInfo2 : list) {
                eventInfo2.recycle();
            }
            this.mDelayedEventQueue.clear();
            this.mDelayedEventQueue = null;
        }
    }

    private void recycleLastEvent() {
        MotionEvent motionEvent = this.mLastEvent;
        if (motionEvent == null) {
            return;
        }
        motionEvent.recycle();
        this.mLastEvent = null;
    }

    private void cacheDelayedMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mLastEvent = MotionEvent.obtain(event);
        MotionEventInfo info = MotionEventInfo.obtain(event, rawEvent, policyFlags);
        if (this.mDelayedEventQueue == null) {
            this.mDelayedEventQueue = new LinkedList();
        }
        this.mDelayedEventQueue.add(info);
    }

    public String toString() {
        return "MagnificationGesturesObserver{, mDelayedEventQueue=" + this.mDelayedEventQueue + '}';
    }
}
