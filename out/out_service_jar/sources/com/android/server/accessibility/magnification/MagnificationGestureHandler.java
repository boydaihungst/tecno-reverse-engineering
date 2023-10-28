package com.android.server.accessibility.magnification;

import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.server.accessibility.AccessibilityTraceManager;
import com.android.server.accessibility.BaseEventStreamTransformation;
import com.android.server.usb.descriptors.UsbACInterface;
import java.util.ArrayDeque;
import java.util.Queue;
/* loaded from: classes.dex */
public abstract class MagnificationGestureHandler extends BaseEventStreamTransformation {
    protected static final boolean DEBUG_ALL;
    protected static final boolean DEBUG_EVENT_STREAM;
    protected final Callback mCallback;
    private final Queue<MotionEvent> mDebugInputEventHistory;
    private final Queue<MotionEvent> mDebugOutputEventHistory;
    protected final boolean mDetectShortcutTrigger;
    protected final boolean mDetectTripleTap;
    protected final int mDisplayId;
    protected final String mLogTag = getClass().getSimpleName();
    private final AccessibilityTraceManager mTrace;

    /* loaded from: classes.dex */
    public interface Callback {
        void onTouchInteractionEnd(int i, int i2);

        void onTouchInteractionStart(int i, int i2);
    }

    public abstract int getMode();

    abstract void handleShortcutTriggered();

    abstract void onMotionEventInternal(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

    static {
        boolean isLoggable = Log.isLoggable("MagnificationGestureHandler", 3);
        DEBUG_ALL = isLoggable;
        DEBUG_EVENT_STREAM = isLoggable | false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public MagnificationGestureHandler(int displayId, boolean detectTripleTap, boolean detectShortcutTrigger, AccessibilityTraceManager trace, Callback callback) {
        this.mDisplayId = displayId;
        this.mDetectTripleTap = detectTripleTap;
        this.mDetectShortcutTrigger = detectShortcutTrigger;
        this.mTrace = trace;
        this.mCallback = callback;
        boolean z = DEBUG_EVENT_STREAM;
        this.mDebugInputEventHistory = z ? new ArrayDeque() : null;
        this.mDebugOutputEventHistory = z ? new ArrayDeque() : null;
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public final void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (DEBUG_ALL) {
            Slog.i(this.mLogTag, "onMotionEvent(" + event + ")");
        }
        if (this.mTrace.isA11yTracingEnabledForTypes(12288L)) {
            this.mTrace.logTrace("MagnificationGestureHandler.onMotionEvent", 12288L, "event=" + event + ";rawEvent=" + rawEvent + ";policyFlags=" + policyFlags);
        }
        if (DEBUG_EVENT_STREAM) {
            storeEventInto(this.mDebugInputEventHistory, event);
        }
        if (shouldDispatchTransformedEvent(event)) {
            dispatchTransformedEvent(event, rawEvent, policyFlags);
            return;
        }
        onMotionEventInternal(event, rawEvent, policyFlags);
        int action = event.getAction();
        if (action == 0) {
            this.mCallback.onTouchInteractionStart(this.mDisplayId, getMode());
        } else if (action == 1 || action == 3) {
            this.mCallback.onTouchInteractionEnd(this.mDisplayId, getMode());
        }
    }

    private boolean shouldDispatchTransformedEvent(MotionEvent event) {
        if ((!this.mDetectTripleTap && !this.mDetectShortcutTrigger) || !event.isFromSource(UsbACInterface.FORMAT_II_AC3)) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void dispatchTransformedEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (DEBUG_EVENT_STREAM) {
            storeEventInto(this.mDebugOutputEventHistory, event);
            try {
                super.onMotionEvent(event, rawEvent, policyFlags);
                return;
            } catch (Exception e) {
                throw new RuntimeException("Exception downstream following input events: " + this.mDebugInputEventHistory + "\nTransformed into output events: " + this.mDebugOutputEventHistory, e);
            }
        }
        super.onMotionEvent(event, rawEvent, policyFlags);
    }

    private static void storeEventInto(Queue<MotionEvent> queue, MotionEvent event) {
        queue.add(MotionEvent.obtain(event));
        while (!queue.isEmpty() && event.getEventTime() - queue.peek().getEventTime() > 5000) {
            queue.remove().recycle();
        }
    }

    public void notifyShortcutTriggered() {
        if (DEBUG_ALL) {
            Slog.i(this.mLogTag, "notifyShortcutTriggered():");
        }
        if (this.mDetectShortcutTrigger) {
            handleShortcutTriggered();
        }
    }
}
