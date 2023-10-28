package com.android.server.accessibility.magnification;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MotionEventDispatcherDelegate {
    private static final boolean DBG;
    private static final String TAG;
    private final EventDispatcher mEventDispatcher;
    private long mLastDelegatedDownEventTime;
    private final int mMultiTapMaxDelay;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface EventDispatcher {
        void dispatchMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i);
    }

    static {
        String simpleName = MotionEventDispatcherDelegate.class.getSimpleName();
        TAG = simpleName;
        DBG = Log.isLoggable(simpleName, 3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MotionEventDispatcherDelegate(Context context, EventDispatcher eventDispatcher) {
        this.mEventDispatcher = eventDispatcher;
        this.mMultiTapMaxDelay = ViewConfiguration.getDoubleTapTimeout() + context.getResources().getInteger(17694938);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDelayedMotionEvents(List<MotionEventInfo> delayedEventQueue, long lastDetectingDownEventTime) {
        if (delayedEventQueue == null) {
            return;
        }
        long offset = Math.min(SystemClock.uptimeMillis() - lastDetectingDownEventTime, this.mMultiTapMaxDelay);
        for (MotionEventInfo info : delayedEventQueue) {
            info.mEvent.setDownTime(info.mEvent.getDownTime() + offset);
            dispatchMotionEvent(info.mEvent, info.mRawEvent, info.mPolicyFlags);
            info.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (event.getActionMasked() == 0) {
            this.mLastDelegatedDownEventTime = event.getDownTime();
            if (DBG) {
                Log.d(TAG, "dispatchMotionEvent mLastDelegatedDownEventTime time = " + this.mLastDelegatedDownEventTime);
            }
        }
        if (DBG) {
            Log.d(TAG, "dispatchMotionEvent original down time = " + event.getDownTime());
        }
        event.setDownTime(this.mLastDelegatedDownEventTime);
        this.mEventDispatcher.dispatchMotionEvent(event, rawEvent, policyFlags);
    }
}
