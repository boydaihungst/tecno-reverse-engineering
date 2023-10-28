package com.android.server.accessibility.gestures;

import android.content.Context;
import android.graphics.Point;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accessibility.EventStreamTransformation;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class EventDispatcher {
    private static final int CLICK_LOCATION_ACCESSIBILITY_FOCUS = 1;
    private static final int CLICK_LOCATION_LAST_TOUCH_EXPLORED = 2;
    private static final int CLICK_LOCATION_NONE = 0;
    private static final String LOG_TAG = "EventDispatcher";
    private final AccessibilityManagerService mAms;
    private Context mContext;
    private int mLongPressingPointerDeltaX;
    private int mLongPressingPointerDeltaY;
    private EventStreamTransformation mReceiver;
    private TouchState mState;
    private int mLongPressingPointerId = -1;
    private final Point mTempPoint = new Point();

    /* JADX INFO: Access modifiers changed from: package-private */
    public EventDispatcher(Context context, AccessibilityManagerService ams, EventStreamTransformation receiver, TouchState state) {
        this.mContext = context;
        this.mAms = ams;
        this.mReceiver = receiver;
        this.mState = state;
    }

    public void setReceiver(EventStreamTransformation receiver) {
        this.mReceiver = receiver;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendMotionEvent(MotionEvent prototype, int action, MotionEvent rawEvent, int pointerIdBits, int policyFlags) {
        MotionEvent event;
        prototype.setAction(action);
        if (pointerIdBits == -1) {
            event = prototype;
        } else {
            try {
                event = prototype.split(pointerIdBits);
            } catch (IllegalArgumentException e) {
                Slog.e(LOG_TAG, "sendMotionEvent: Failed to split motion event: " + e);
                return;
            }
        }
        if (action == 0) {
            event.setDownTime(event.getEventTime());
        } else {
            event.setDownTime(this.mState.getLastInjectedDownEventTime());
        }
        if (this.mLongPressingPointerId >= 0) {
            event = offsetEvent(event, -this.mLongPressingPointerDeltaX, -this.mLongPressingPointerDeltaY);
        }
        int policyFlags2 = policyFlags | 1073741824;
        EventStreamTransformation eventStreamTransformation = this.mReceiver;
        if (eventStreamTransformation == null) {
            Slog.e(LOG_TAG, "Error sending event: no receiver specified.");
        } else {
            eventStreamTransformation.onMotionEvent(event, rawEvent, policyFlags2);
        }
        this.mState.onInjectedMotionEvent(event);
        if (event != prototype) {
            event.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendAccessibilityEvent(int type) {
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mContext);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(type);
            event.setWindowId(this.mAms.getActiveWindowId());
            accessibilityManager.sendAccessibilityEvent(event);
        }
        this.mState.onInjectedAccessibilityEvent(type);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("=========================");
        builder.append("\nDown pointers #");
        builder.append(Integer.bitCount(this.mState.getInjectedPointersDown()));
        builder.append(" [ ");
        for (int i = 0; i < 32; i++) {
            if (this.mState.isInjectedPointerDown(i)) {
                builder.append(i);
                builder.append(" ");
            }
        }
        builder.append("]");
        builder.append("\n=========================");
        return builder.toString();
    }

    private MotionEvent offsetEvent(MotionEvent event, int offsetX, int offsetY) {
        if (offsetX != 0 || offsetY != 0) {
            int remappedIndex = event.findPointerIndex(this.mLongPressingPointerId);
            int pointerCount = event.getPointerCount();
            MotionEvent.PointerProperties[] props = MotionEvent.PointerProperties.createArray(pointerCount);
            MotionEvent.PointerCoords[] coords = MotionEvent.PointerCoords.createArray(pointerCount);
            for (int i = 0; i < pointerCount; i++) {
                event.getPointerProperties(i, props[i]);
                event.getPointerCoords(i, coords[i]);
                if (i == remappedIndex) {
                    coords[i].x += offsetX;
                    coords[i].y += offsetY;
                }
            }
            return MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), event.getPointerCount(), props, coords, event.getMetaState(), event.getButtonState(), 1.0f, 1.0f, event.getDeviceId(), event.getEdgeFlags(), event.getSource(), event.getDisplayId(), event.getFlags());
        }
        return event;
    }

    private int computeInjectionAction(int actionMasked, int pointerIndex) {
        switch (actionMasked) {
            case 0:
            case 5:
                if (this.mState.getInjectedPointerDownCount() == 0) {
                    return 0;
                }
                return (pointerIndex << 8) | 5;
            case 6:
                if (this.mState.getInjectedPointerDownCount() == 1) {
                    return 1;
                }
                return (pointerIndex << 8) | 6;
            default:
                return actionMasked;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDownForAllNotInjectedPointers(MotionEvent prototype, int policyFlags) {
        int pointerIdBits = 0;
        int pointerCount = prototype.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            try {
                int pointerId = prototype.getPointerId(i);
                if (!this.mState.isInjectedPointerDown(pointerId)) {
                    pointerIdBits |= 1 << pointerId;
                    int action = computeInjectionAction(0, i);
                    sendMotionEvent(prototype, action, this.mState.getLastReceivedEvent(), pointerIdBits, policyFlags);
                }
            } catch (IllegalArgumentException e) {
                Slog.e(LOG_TAG, "pointerIndex out of range, skip this motion event");
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendDownForAllNotInjectedPointersWithOriginalDown(MotionEvent prototype, int policyFlags) {
        int pointerIdBits = 0;
        int pointerCount = prototype.getPointerCount();
        MotionEvent event = computeInjectionDownEvent(prototype);
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = prototype.getPointerId(i);
            if (!this.mState.isInjectedPointerDown(pointerId)) {
                pointerIdBits |= 1 << pointerId;
                int action = computeInjectionAction(0, i);
                sendMotionEvent(event, action, this.mState.getLastReceivedEvent(), pointerIdBits, policyFlags);
            }
        }
    }

    private MotionEvent computeInjectionDownEvent(MotionEvent prototype) {
        int pointerCount = prototype.getPointerCount();
        if (pointerCount != this.mState.getReceivedPointerTracker().getReceivedPointerDownCount()) {
            Slog.w(LOG_TAG, "The pointer count doesn't match the received count.");
            return MotionEvent.obtain(prototype);
        }
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointerCount];
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = prototype.getPointerId(i);
            float x = this.mState.getReceivedPointerTracker().getReceivedPointerDownX(pointerId);
            float y = this.mState.getReceivedPointerTracker().getReceivedPointerDownY(pointerId);
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = x;
            coords[i].y = y;
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = pointerId;
            properties[i].toolType = 1;
        }
        MotionEvent event = MotionEvent.obtain(prototype.getDownTime(), prototype.getDownTime(), prototype.getAction(), pointerCount, properties, coords, prototype.getMetaState(), prototype.getButtonState(), prototype.getXPrecision(), prototype.getYPrecision(), prototype.getDeviceId(), prototype.getEdgeFlags(), prototype.getSource(), prototype.getFlags());
        return event;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendUpForInjectedDownPointers(MotionEvent prototype, int policyFlags) {
        int pointerIdBits = prototype.getPointerIdBits();
        int pointerCount = prototype.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = prototype.getPointerId(i);
            if (this.mState.isInjectedPointerDown(pointerId)) {
                int action = computeInjectionAction(6, i);
                sendMotionEvent(prototype, action, this.mState.getLastReceivedEvent(), pointerIdBits, policyFlags);
                pointerIdBits &= ~(1 << pointerId);
            }
        }
    }

    public boolean longPressWithTouchEvents(MotionEvent event, int policyFlags) {
        Point clickLocation = this.mTempPoint;
        int result = computeClickLocation(clickLocation);
        if (result == 0 || event == null) {
            return false;
        }
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        this.mLongPressingPointerId = pointerId;
        this.mLongPressingPointerDeltaX = ((int) event.getX(pointerIndex)) - clickLocation.x;
        this.mLongPressingPointerDeltaY = ((int) event.getY(pointerIndex)) - clickLocation.y;
        sendDownForAllNotInjectedPointers(event, policyFlags);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mLongPressingPointerId = -1;
        this.mLongPressingPointerDeltaX = 0;
        this.mLongPressingPointerDeltaY = 0;
    }

    public void clickWithTouchEvents(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        int pointerIndex = event.getActionIndex();
        event.getPointerId(pointerIndex);
        Point clickLocation = this.mTempPoint;
        int result = computeClickLocation(clickLocation);
        if (result == 0) {
            Slog.e(LOG_TAG, "Unable to compute click location.");
            return;
        }
        MotionEvent.PointerProperties[] properties = {new MotionEvent.PointerProperties()};
        event.getPointerProperties(pointerIndex, properties[0]);
        MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
        coords[0].x = clickLocation.x;
        coords[0].y = clickLocation.y;
        MotionEvent clickEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), 0, 1, properties, coords, 0, 0, 1.0f, 1.0f, event.getDeviceId(), 0, event.getSource(), event.getDisplayId(), event.getFlags());
        boolean targetAccessibilityFocus = result == 1;
        sendActionDownAndUp(clickEvent, rawEvent, policyFlags, targetAccessibilityFocus);
        clickEvent.recycle();
    }

    private int computeClickLocation(Point outLocation) {
        if (this.mState.getLastInjectedHoverEventForClick() != null) {
            int lastExplorePointerIndex = this.mState.getLastInjectedHoverEventForClick().getActionIndex();
            outLocation.x = (int) this.mState.getLastInjectedHoverEventForClick().getX(lastExplorePointerIndex);
            outLocation.y = (int) this.mState.getLastInjectedHoverEventForClick().getY(lastExplorePointerIndex);
            if (!this.mAms.accessibilityFocusOnlyInActiveWindow() || this.mState.getLastTouchedWindowId() == this.mAms.getActiveWindowId()) {
                return this.mAms.getAccessibilityFocusClickPointInScreen(outLocation) ? 1 : 2;
            }
        }
        return this.mAms.getAccessibilityFocusClickPointInScreen(outLocation) ? 1 : 0;
    }

    private void sendActionDownAndUp(MotionEvent prototype, MotionEvent rawEvent, int policyFlags, boolean targetAccessibilityFocus) {
        int pointerId = prototype.getPointerId(prototype.getActionIndex());
        int pointerIdBits = 1 << pointerId;
        prototype.setTargetAccessibilityFocus(targetAccessibilityFocus);
        sendMotionEvent(prototype, 0, rawEvent, pointerIdBits, policyFlags);
        prototype.setTargetAccessibilityFocus(targetAccessibilityFocus);
        sendMotionEvent(prototype, 1, rawEvent, pointerIdBits, policyFlags);
    }
}
