package com.android.server.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.server.usb.descriptors.UsbACInterface;
/* loaded from: classes.dex */
public class AutoclickController extends BaseEventStreamTransformation {
    private static final String LOG_TAG = AutoclickController.class.getSimpleName();
    private ClickDelayObserver mClickDelayObserver;
    private ClickScheduler mClickScheduler;
    private final Context mContext;
    private final AccessibilityTraceManager mTrace;
    private final int mUserId;

    public AutoclickController(Context context, int userId, AccessibilityTraceManager trace) {
        this.mTrace = trace;
        this.mContext = context;
        this.mUserId = userId;
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mTrace.isA11yTracingEnabledForTypes(4096L)) {
            this.mTrace.logTrace(LOG_TAG + ".onMotionEvent", 4096L, "event=" + event + ";rawEvent=" + rawEvent + ";policyFlags=" + policyFlags);
        }
        if (event.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            if (this.mClickScheduler == null) {
                Handler handler = new Handler(this.mContext.getMainLooper());
                this.mClickScheduler = new ClickScheduler(handler, 600);
                ClickDelayObserver clickDelayObserver = new ClickDelayObserver(this.mUserId, handler);
                this.mClickDelayObserver = clickDelayObserver;
                clickDelayObserver.start(this.mContext.getContentResolver(), this.mClickScheduler);
            }
            handleMouseMotion(event, policyFlags);
        } else {
            ClickScheduler clickScheduler = this.mClickScheduler;
            if (clickScheduler != null) {
                clickScheduler.cancel();
            }
        }
        super.onMotionEvent(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onKeyEvent(KeyEvent event, int policyFlags) {
        if (this.mTrace.isA11yTracingEnabledForTypes(4096L)) {
            this.mTrace.logTrace(LOG_TAG + ".onKeyEvent", 4096L, "event=" + event + ";policyFlags=" + policyFlags);
        }
        if (this.mClickScheduler != null) {
            if (KeyEvent.isModifierKey(event.getKeyCode())) {
                this.mClickScheduler.updateMetaState(event.getMetaState());
            } else {
                this.mClickScheduler.cancel();
            }
        }
        super.onKeyEvent(event, policyFlags);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void clearEvents(int inputSource) {
        ClickScheduler clickScheduler;
        if (inputSource == 8194 && (clickScheduler = this.mClickScheduler) != null) {
            clickScheduler.cancel();
        }
        super.clearEvents(inputSource);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onDestroy() {
        ClickDelayObserver clickDelayObserver = this.mClickDelayObserver;
        if (clickDelayObserver != null) {
            clickDelayObserver.stop();
            this.mClickDelayObserver = null;
        }
        ClickScheduler clickScheduler = this.mClickScheduler;
        if (clickScheduler != null) {
            clickScheduler.cancel();
            this.mClickScheduler = null;
        }
    }

    private void handleMouseMotion(MotionEvent event, int policyFlags) {
        switch (event.getActionMasked()) {
            case 7:
                if (event.getPointerCount() == 1) {
                    this.mClickScheduler.update(event, policyFlags);
                    return;
                } else {
                    this.mClickScheduler.cancel();
                    return;
                }
            case 8:
            default:
                this.mClickScheduler.cancel();
                return;
            case 9:
            case 10:
                return;
        }
    }

    /* loaded from: classes.dex */
    private static final class ClickDelayObserver extends ContentObserver {
        private final Uri mAutoclickDelaySettingUri;
        private ClickScheduler mClickScheduler;
        private ContentResolver mContentResolver;
        private final int mUserId;

        public ClickDelayObserver(int userId, Handler handler) {
            super(handler);
            this.mAutoclickDelaySettingUri = Settings.Secure.getUriFor("accessibility_autoclick_delay");
            this.mUserId = userId;
        }

        public void start(ContentResolver contentResolver, ClickScheduler clickScheduler) {
            if (this.mContentResolver != null || this.mClickScheduler != null) {
                throw new IllegalStateException("Observer already started.");
            }
            if (contentResolver == null) {
                throw new NullPointerException("contentResolver not set.");
            }
            if (clickScheduler == null) {
                throw new NullPointerException("clickScheduler not set.");
            }
            this.mContentResolver = contentResolver;
            this.mClickScheduler = clickScheduler;
            contentResolver.registerContentObserver(this.mAutoclickDelaySettingUri, false, this, this.mUserId);
            onChange(true, this.mAutoclickDelaySettingUri);
        }

        public void stop() {
            ContentResolver contentResolver = this.mContentResolver;
            if (contentResolver == null || this.mClickScheduler == null) {
                throw new IllegalStateException("ClickDelayObserver not started.");
            }
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (this.mAutoclickDelaySettingUri.equals(uri)) {
                int delay = Settings.Secure.getIntForUser(this.mContentResolver, "accessibility_autoclick_delay", 600, this.mUserId);
                this.mClickScheduler.updateDelay(delay);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ClickScheduler implements Runnable {
        private static final double MOVEMENT_SLOPE = 20.0d;
        private boolean mActive;
        private MotionEvent.PointerCoords mAnchorCoords;
        private int mDelay;
        private int mEventPolicyFlags;
        private Handler mHandler;
        private MotionEvent mLastMotionEvent = null;
        private int mMetaState;
        private long mScheduledClickTime;
        private MotionEvent.PointerCoords[] mTempPointerCoords;
        private MotionEvent.PointerProperties[] mTempPointerProperties;

        public ClickScheduler(Handler handler, int delay) {
            this.mHandler = handler;
            resetInternalState();
            this.mDelay = delay;
            this.mAnchorCoords = new MotionEvent.PointerCoords();
        }

        @Override // java.lang.Runnable
        public void run() {
            long now = SystemClock.uptimeMillis();
            long j = this.mScheduledClickTime;
            if (now < j) {
                this.mHandler.postDelayed(this, j - now);
                return;
            }
            sendClick();
            resetInternalState();
        }

        public void update(MotionEvent event, int policyFlags) {
            this.mMetaState = event.getMetaState();
            boolean moved = detectMovement(event);
            cacheLastEvent(event, policyFlags, this.mLastMotionEvent == null || moved);
            if (moved) {
                rescheduleClick(this.mDelay);
            }
        }

        public void cancel() {
            if (!this.mActive) {
                return;
            }
            resetInternalState();
            this.mHandler.removeCallbacks(this);
        }

        public void updateMetaState(int state) {
            this.mMetaState = state;
        }

        public void updateDelay(int delay) {
            this.mDelay = delay;
        }

        private void rescheduleClick(int delay) {
            long clickTime = SystemClock.uptimeMillis() + delay;
            boolean z = this.mActive;
            if (z && clickTime > this.mScheduledClickTime) {
                this.mScheduledClickTime = clickTime;
                return;
            }
            if (z) {
                this.mHandler.removeCallbacks(this);
            }
            this.mActive = true;
            this.mScheduledClickTime = clickTime;
            this.mHandler.postDelayed(this, delay);
        }

        private void cacheLastEvent(MotionEvent event, int policyFlags, boolean useAsAnchor) {
            MotionEvent motionEvent = this.mLastMotionEvent;
            if (motionEvent != null) {
                motionEvent.recycle();
            }
            MotionEvent obtain = MotionEvent.obtain(event);
            this.mLastMotionEvent = obtain;
            this.mEventPolicyFlags = policyFlags;
            if (useAsAnchor) {
                int pointerIndex = obtain.getActionIndex();
                this.mLastMotionEvent.getPointerCoords(pointerIndex, this.mAnchorCoords);
            }
        }

        private void resetInternalState() {
            this.mActive = false;
            MotionEvent motionEvent = this.mLastMotionEvent;
            if (motionEvent != null) {
                motionEvent.recycle();
                this.mLastMotionEvent = null;
            }
            this.mScheduledClickTime = -1L;
        }

        private boolean detectMovement(MotionEvent event) {
            if (this.mLastMotionEvent == null) {
                return false;
            }
            int pointerIndex = event.getActionIndex();
            float deltaX = this.mAnchorCoords.x - event.getX(pointerIndex);
            float deltaY = this.mAnchorCoords.y - event.getY(pointerIndex);
            double delta = Math.hypot(deltaX, deltaY);
            return delta > 20.0d;
        }

        private void sendClick() {
            if (this.mLastMotionEvent == null || AutoclickController.this.getNext() == null) {
                return;
            }
            int pointerIndex = this.mLastMotionEvent.getActionIndex();
            if (this.mTempPointerProperties == null) {
                this.mTempPointerProperties = r2;
                MotionEvent.PointerProperties[] pointerPropertiesArr = {new MotionEvent.PointerProperties()};
            }
            this.mLastMotionEvent.getPointerProperties(pointerIndex, this.mTempPointerProperties[0]);
            if (this.mTempPointerCoords == null) {
                this.mTempPointerCoords = r2;
                MotionEvent.PointerCoords[] pointerCoordsArr = {new MotionEvent.PointerCoords()};
            }
            this.mLastMotionEvent.getPointerCoords(pointerIndex, this.mTempPointerCoords[0]);
            long now = SystemClock.uptimeMillis();
            MotionEvent downEvent = MotionEvent.obtain(now, now, 0, 1, this.mTempPointerProperties, this.mTempPointerCoords, this.mMetaState, 1, 1.0f, 1.0f, this.mLastMotionEvent.getDeviceId(), 0, this.mLastMotionEvent.getSource(), this.mLastMotionEvent.getFlags());
            MotionEvent pressEvent = MotionEvent.obtain(downEvent);
            pressEvent.setAction(11);
            pressEvent.setActionButton(1);
            MotionEvent releaseEvent = MotionEvent.obtain(downEvent);
            releaseEvent.setAction(12);
            releaseEvent.setActionButton(1);
            releaseEvent.setButtonState(0);
            MotionEvent upEvent = MotionEvent.obtain(downEvent);
            upEvent.setAction(1);
            upEvent.setButtonState(0);
            AutoclickController.super.onMotionEvent(downEvent, downEvent, this.mEventPolicyFlags);
            downEvent.recycle();
            AutoclickController.super.onMotionEvent(pressEvent, pressEvent, this.mEventPolicyFlags);
            pressEvent.recycle();
            AutoclickController.super.onMotionEvent(releaseEvent, releaseEvent, this.mEventPolicyFlags);
            releaseEvent.recycle();
            AutoclickController.super.onMotionEvent(upEvent, upEvent, this.mEventPolicyFlags);
            upEvent.recycle();
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ClickScheduler: { active=").append(this.mActive);
            builder.append(", delay=").append(this.mDelay);
            builder.append(", scheduledClickTime=").append(this.mScheduledClickTime);
            builder.append(", anchor={x:").append(this.mAnchorCoords.x);
            builder.append(", y:").append(this.mAnchorCoords.y).append("}");
            builder.append(", metastate=").append(this.mMetaState);
            builder.append(", policyFlags=").append(this.mEventPolicyFlags);
            builder.append(", lastMotionEvent=").append(this.mLastMotionEvent);
            builder.append(" }");
            return builder.toString();
        }
    }
}
