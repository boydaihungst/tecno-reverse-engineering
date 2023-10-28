package com.android.server.accessibility;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Pools;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.policy.WindowManagerPolicy;
/* loaded from: classes.dex */
public class KeyboardInterceptor extends BaseEventStreamTransformation implements Handler.Callback {
    private static final String LOG_TAG = "KeyboardInterceptor";
    private static final int MESSAGE_PROCESS_QUEUED_EVENTS = 1;
    private final AccessibilityManagerService mAms;
    private KeyEventHolder mEventQueueEnd;
    private KeyEventHolder mEventQueueStart;
    private final Handler mHandler;
    private final WindowManagerPolicy mPolicy;

    public KeyboardInterceptor(AccessibilityManagerService service, WindowManagerPolicy policy) {
        this.mAms = service;
        this.mPolicy = policy;
        this.mHandler = new Handler(this);
    }

    public KeyboardInterceptor(AccessibilityManagerService service, WindowManagerPolicy policy, Handler handler) {
        this.mAms = service;
        this.mPolicy = policy;
        this.mHandler = handler;
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onKeyEvent(KeyEvent event, int policyFlags) {
        if (this.mAms.getTraceManager().isA11yTracingEnabledForTypes(4096L)) {
            this.mAms.getTraceManager().logTrace("KeyboardInterceptor.onKeyEvent", 4096L, "event=" + event + ";policyFlags=" + policyFlags);
        }
        long eventDelay = getEventDelay(event, policyFlags);
        if (eventDelay < 0) {
            return;
        }
        if (eventDelay > 0 || this.mEventQueueStart != null) {
            addEventToQueue(event, policyFlags, eventDelay);
        } else {
            this.mAms.notifyKeyEvent(event, policyFlags);
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        if (msg.what != 1) {
            Slog.e(LOG_TAG, "Unexpected message type");
            return false;
        }
        processQueuedEvents();
        if (this.mEventQueueStart != null) {
            scheduleProcessQueuedEvents();
        }
        return true;
    }

    private void addEventToQueue(KeyEvent event, int policyFlags, long delay) {
        long dispatchTime = SystemClock.uptimeMillis() + delay;
        if (this.mEventQueueStart == null) {
            KeyEventHolder obtain = KeyEventHolder.obtain(event, policyFlags, dispatchTime);
            this.mEventQueueStart = obtain;
            this.mEventQueueEnd = obtain;
            scheduleProcessQueuedEvents();
            return;
        }
        KeyEventHolder holder = KeyEventHolder.obtain(event, policyFlags, dispatchTime);
        holder.next = this.mEventQueueStart;
        this.mEventQueueStart.previous = holder;
        this.mEventQueueStart = holder;
    }

    private void scheduleProcessQueuedEvents() {
        if (!this.mHandler.sendEmptyMessageAtTime(1, this.mEventQueueEnd.dispatchTime)) {
            Slog.e(LOG_TAG, "Failed to schedule key event");
        }
    }

    private void processQueuedEvents() {
        long currentTime = SystemClock.uptimeMillis();
        while (true) {
            KeyEventHolder keyEventHolder = this.mEventQueueEnd;
            if (keyEventHolder != null && keyEventHolder.dispatchTime <= currentTime) {
                long eventDelay = getEventDelay(this.mEventQueueEnd.event, this.mEventQueueEnd.policyFlags);
                if (eventDelay > 0) {
                    this.mEventQueueEnd.dispatchTime = currentTime + eventDelay;
                    return;
                }
                if (eventDelay == 0) {
                    this.mAms.notifyKeyEvent(this.mEventQueueEnd.event, this.mEventQueueEnd.policyFlags);
                }
                KeyEventHolder eventToBeRecycled = this.mEventQueueEnd;
                KeyEventHolder keyEventHolder2 = this.mEventQueueEnd.previous;
                this.mEventQueueEnd = keyEventHolder2;
                if (keyEventHolder2 != null) {
                    keyEventHolder2.next = null;
                }
                eventToBeRecycled.recycle();
                if (this.mEventQueueEnd == null) {
                    this.mEventQueueStart = null;
                }
            } else {
                return;
            }
        }
    }

    private long getEventDelay(KeyEvent event, int policyFlags) {
        int keyCode = event.getKeyCode();
        if (keyCode == 25 || keyCode == 24) {
            return this.mPolicy.interceptKeyBeforeDispatching(null, event, policyFlags);
        }
        return 0L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class KeyEventHolder {
        private static final int MAX_POOL_SIZE = 32;
        private static final Pools.SimplePool<KeyEventHolder> sPool = new Pools.SimplePool<>(32);
        public long dispatchTime;
        public KeyEvent event;
        public KeyEventHolder next;
        public int policyFlags;
        public KeyEventHolder previous;

        private KeyEventHolder() {
        }

        public static KeyEventHolder obtain(KeyEvent event, int policyFlags, long dispatchTime) {
            KeyEventHolder holder = (KeyEventHolder) sPool.acquire();
            if (holder == null) {
                holder = new KeyEventHolder();
            }
            holder.event = KeyEvent.obtain(event);
            holder.policyFlags = policyFlags;
            holder.dispatchTime = dispatchTime;
            return holder;
        }

        public void recycle() {
            this.event.recycle();
            this.event = null;
            this.policyFlags = 0;
            this.dispatchTime = 0L;
            this.next = null;
            this.previous = null;
            sPool.release(this);
        }
    }
}
