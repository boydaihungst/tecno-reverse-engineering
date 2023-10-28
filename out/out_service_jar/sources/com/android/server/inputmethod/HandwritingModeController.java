package com.android.server.inputmethod;

import android.hardware.input.InputManagerInternal;
import android.os.IBinder;
import android.os.Looper;
import android.util.Slog;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HandwritingModeController {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final boolean DEBUG = true;
    private static final int EVENT_BUFFER_SIZE = 100;
    public static final String TAG = HandwritingModeController.class.getSimpleName();
    private List<MotionEvent> mHandwritingBuffer;
    private InputEventReceiver mHandwritingEventReceiver;
    private HandwritingEventReceiverSurface mHandwritingSurface;
    private Runnable mInkWindowInitRunnable;
    private final Looper mLooper;
    private boolean mRecordingGesture;
    private int mCurrentDisplayId = -1;
    private final InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
    private final WindowManagerInternal mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    private int mCurrentRequestId = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandwritingModeController(Looper uiThreadLooper, Runnable inkWindowInitRunnable) {
        this.mLooper = uiThreadLooper;
        this.mInkWindowInitRunnable = inkWindowInitRunnable;
    }

    private static boolean isStylusEvent(MotionEvent event) {
        if (event.isFromSource(16386)) {
            int tool = event.getToolType(0);
            return tool == 2 || tool == 4;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeHandwritingSpy(int displayId) {
        reset(displayId == this.mCurrentDisplayId);
        this.mCurrentDisplayId = displayId;
        if (this.mHandwritingBuffer == null) {
            this.mHandwritingBuffer = new ArrayList(100);
        }
        String str = TAG;
        Slog.d(str, "Initializing handwriting spy monitor for display: " + displayId);
        String name = "stylus-handwriting-event-receiver-" + displayId;
        InputChannel channel = this.mInputManagerInternal.createInputChannel(name);
        Objects.requireNonNull(channel, "Failed to create input channel");
        HandwritingEventReceiverSurface handwritingEventReceiverSurface = this.mHandwritingSurface;
        SurfaceControl surface = handwritingEventReceiverSurface != null ? handwritingEventReceiverSurface.getSurface() : this.mWindowManagerInternal.getHandwritingSurfaceForDisplay(displayId);
        if (surface == null) {
            Slog.e(str, "Failed to create input surface");
            return;
        }
        this.mHandwritingSurface = new HandwritingEventReceiverSurface(name, displayId, surface, channel);
        this.mHandwritingEventReceiver = new BatchedInputEventReceiver.SimpleBatchedInputEventReceiver(channel.dup(), this.mLooper, Choreographer.getInstance(), new BatchedInputEventReceiver.SimpleBatchedInputEventReceiver.InputEventListener() { // from class: com.android.server.inputmethod.HandwritingModeController$$ExternalSyntheticLambda1
            public final boolean onInputEvent(InputEvent inputEvent) {
                boolean onInputEvent;
                onInputEvent = HandwritingModeController.this.onInputEvent(inputEvent);
                return onInputEvent;
            }
        });
        this.mCurrentRequestId++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OptionalInt getCurrentRequestId() {
        if (this.mHandwritingSurface == null) {
            Slog.e(TAG, "Cannot get requestId: Handwriting was not initialized.");
            return OptionalInt.empty();
        }
        return OptionalInt.of(this.mCurrentRequestId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStylusGestureOngoing() {
        return this.mRecordingGesture;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandwritingSession startHandwritingSession(int requestId, int imePid, int imeUid, IBinder focusedWindowToken) {
        if (this.mHandwritingSurface == null) {
            Slog.e(TAG, "Cannot start handwriting session: Handwriting was not initialized.");
            return null;
        } else if (requestId != this.mCurrentRequestId) {
            Slog.e(TAG, "Cannot start handwriting session: Invalid request id: " + requestId);
            return null;
        } else if (!this.mRecordingGesture || this.mHandwritingBuffer.isEmpty()) {
            Slog.e(TAG, "Cannot start handwriting session: No stylus gesture is being recorded.");
            return null;
        } else {
            Objects.requireNonNull(this.mHandwritingEventReceiver, "Handwriting session was already transferred to IME.");
            MotionEvent downEvent = this.mHandwritingBuffer.get(0);
            if (!this.mWindowManagerInternal.isPointInsideWindow(focusedWindowToken, this.mCurrentDisplayId, downEvent.getRawX(), downEvent.getRawY())) {
                Slog.e(TAG, "Cannot start handwriting session: Stylus gesture did not start inside the focused window.");
                return null;
            }
            Slog.d(TAG, "Starting handwriting session in display: " + this.mCurrentDisplayId);
            this.mInputManagerInternal.pilferPointers(this.mHandwritingSurface.getInputChannel().getToken());
            this.mHandwritingEventReceiver.dispose();
            this.mHandwritingEventReceiver = null;
            this.mRecordingGesture = false;
            if (this.mHandwritingSurface.isIntercepting()) {
                throw new IllegalStateException("Handwriting surface should not be already intercepting.");
            }
            this.mHandwritingSurface.startIntercepting(imePid, imeUid);
            return new HandwritingSession(this.mCurrentRequestId, this.mHandwritingSurface.getInputChannel(), this.mHandwritingBuffer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        reset(false);
    }

    private void reset(boolean reinitializing) {
        InputEventReceiver inputEventReceiver = this.mHandwritingEventReceiver;
        if (inputEventReceiver != null) {
            inputEventReceiver.dispose();
            this.mHandwritingEventReceiver = null;
        }
        List<MotionEvent> list = this.mHandwritingBuffer;
        if (list != null) {
            list.forEach(new Consumer() { // from class: com.android.server.inputmethod.HandwritingModeController$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((MotionEvent) obj).recycle();
                }
            });
            this.mHandwritingBuffer.clear();
            if (!reinitializing) {
                this.mHandwritingBuffer = null;
            }
        }
        HandwritingEventReceiverSurface handwritingEventReceiverSurface = this.mHandwritingSurface;
        if (handwritingEventReceiverSurface != null) {
            handwritingEventReceiverSurface.getInputChannel().dispose();
            if (!reinitializing) {
                this.mHandwritingSurface.remove();
                this.mHandwritingSurface = null;
            }
        }
        this.mRecordingGesture = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean onInputEvent(InputEvent ev) {
        if (this.mHandwritingEventReceiver == null) {
            throw new IllegalStateException("Input Event should not be processed when IME has the spy channel.");
        }
        if (!(ev instanceof MotionEvent)) {
            Slog.wtf(TAG, "Received non-motion event in stylus monitor.");
            return false;
        }
        MotionEvent event = (MotionEvent) ev;
        if (isStylusEvent(event)) {
            if (event.getDisplayId() != this.mCurrentDisplayId) {
                Slog.wtf(TAG, "Received stylus event associated with the incorrect display.");
                return false;
            }
            onStylusEvent(event);
            return true;
        }
        return false;
    }

    private void onStylusEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (this.mInkWindowInitRunnable != null && (action == 9 || event.getAction() == 9)) {
            this.mInkWindowInitRunnable.run();
            this.mInkWindowInitRunnable = null;
        }
        if (action == 1 || action == 3) {
            this.mRecordingGesture = false;
            this.mHandwritingBuffer.clear();
            return;
        }
        if (action == 0) {
            this.mRecordingGesture = true;
        }
        if (!this.mRecordingGesture) {
            return;
        }
        if (this.mHandwritingBuffer.size() >= 100) {
            Slog.w(TAG, "Current gesture exceeds the buffer capacity. The rest of the gesture will not be recorded.");
            this.mRecordingGesture = false;
            return;
        }
        this.mHandwritingBuffer.add(MotionEvent.obtain(event));
    }

    /* loaded from: classes.dex */
    static final class HandwritingSession {
        private final InputChannel mHandwritingChannel;
        private final List<MotionEvent> mRecordedEvents;
        private final int mRequestId;

        private HandwritingSession(int requestId, InputChannel handwritingChannel, List<MotionEvent> recordedEvents) {
            this.mRequestId = requestId;
            this.mHandwritingChannel = handwritingChannel;
            this.mRecordedEvents = recordedEvents;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getRequestId() {
            return this.mRequestId;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public InputChannel getHandwritingChannel() {
            return this.mHandwritingChannel;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public List<MotionEvent> getRecordedEvents() {
            return this.mRecordedEvents;
        }
    }
}
