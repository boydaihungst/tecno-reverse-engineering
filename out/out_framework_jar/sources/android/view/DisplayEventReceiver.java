package android.view;

import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import dalvik.annotation.optimization.FastNative;
import java.lang.ref.WeakReference;
/* loaded from: classes3.dex */
public abstract class DisplayEventReceiver {
    public static final int EVENT_REGISTRATION_FRAME_RATE_OVERRIDE_FLAG = 2;
    public static final int EVENT_REGISTRATION_MODE_CHANGED_FLAG = 1;
    private static final String TAG = "DisplayEventReceiver";
    public static final int VSYNC_SOURCE_APP = 0;
    public static final int VSYNC_SOURCE_SURFACE_FLINGER = 1;
    private MessageQueue mMessageQueue;
    private long mReceiverPtr;

    private static native void nativeDispose(long j);

    private static native VsyncEventData nativeGetLatestVsyncEventData(long j);

    private static native long nativeInit(WeakReference<DisplayEventReceiver> weakReference, MessageQueue messageQueue, int i, int i2);

    @FastNative
    private static native void nativeScheduleVsync(long j);

    public DisplayEventReceiver(Looper looper) {
        this(looper, 0, 0);
    }

    public DisplayEventReceiver(Looper looper, int vsyncSource, int eventRegistration) {
        if (looper == null) {
            throw new IllegalArgumentException("looper must not be null");
        }
        this.mMessageQueue = looper.getQueue();
        this.mReceiverPtr = nativeInit(new WeakReference(this), this.mMessageQueue, vsyncSource, eventRegistration);
    }

    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    public void dispose() {
        dispose(false);
    }

    private void dispose(boolean finalized) {
        long j = this.mReceiverPtr;
        if (j != 0) {
            nativeDispose(j);
            this.mReceiverPtr = 0L;
        }
        this.mMessageQueue = null;
    }

    /* loaded from: classes3.dex */
    public static final class VsyncEventData {
        static final FrameTimeline[] INVALID_FRAME_TIMELINES = {new FrameTimeline(-1, Long.MAX_VALUE, Long.MAX_VALUE)};
        public final long frameInterval;
        public final FrameTimeline[] frameTimelines;
        public final int preferredFrameTimelineIndex;

        /* loaded from: classes3.dex */
        public static class FrameTimeline {
            public final long deadline;
            public final long expectedPresentTime;
            public final long vsyncId;

            FrameTimeline(long vsyncId, long expectedPresentTime, long deadline) {
                this.vsyncId = vsyncId;
                this.expectedPresentTime = expectedPresentTime;
                this.deadline = deadline;
            }
        }

        VsyncEventData(FrameTimeline[] frameTimelines, int preferredFrameTimelineIndex, long frameInterval) {
            this.frameTimelines = frameTimelines;
            this.preferredFrameTimelineIndex = preferredFrameTimelineIndex;
            this.frameInterval = frameInterval;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public VsyncEventData() {
            this.frameInterval = -1L;
            this.frameTimelines = INVALID_FRAME_TIMELINES;
            this.preferredFrameTimelineIndex = 0;
        }

        public FrameTimeline preferredFrameTimeline() {
            return this.frameTimelines[this.preferredFrameTimelineIndex];
        }
    }

    public void onVsync(long timestampNanos, long physicalDisplayId, int frame, VsyncEventData vsyncEventData) {
    }

    public void onHotplug(long timestampNanos, long physicalDisplayId, boolean connected) {
    }

    public void onModeChanged(long timestampNanos, long physicalDisplayId, int modeId) {
    }

    /* loaded from: classes3.dex */
    public static class FrameRateOverride {
        public final float frameRateHz;
        public final int uid;

        public FrameRateOverride(int uid, float frameRateHz) {
            this.uid = uid;
            this.frameRateHz = frameRateHz;
        }

        public String toString() {
            return "{uid=" + this.uid + " frameRateHz=" + this.frameRateHz + "}";
        }
    }

    public void onFrameRateOverridesChanged(long timestampNanos, long physicalDisplayId, FrameRateOverride[] overrides) {
    }

    public void scheduleVsync() {
        long j = this.mReceiverPtr;
        if (j == 0) {
            Log.w(TAG, "Attempted to schedule a vertical sync pulse but the display event receiver has already been disposed.");
        } else {
            nativeScheduleVsync(j);
        }
    }

    VsyncEventData getLatestVsyncEventData() {
        return nativeGetLatestVsyncEventData(this.mReceiverPtr);
    }

    private void dispatchVsync(long timestampNanos, long physicalDisplayId, int frame, VsyncEventData vsyncEventData) {
        onVsync(timestampNanos, physicalDisplayId, frame, vsyncEventData);
    }

    private void dispatchHotplug(long timestampNanos, long physicalDisplayId, boolean connected) {
        onHotplug(timestampNanos, physicalDisplayId, connected);
    }

    private void dispatchModeChanged(long timestampNanos, long physicalDisplayId, int modeId) {
        onModeChanged(timestampNanos, physicalDisplayId, modeId);
    }

    private void dispatchFrameRateOverrides(long timestampNanos, long physicalDisplayId, FrameRateOverride[] overrides) {
        onFrameRateOverridesChanged(timestampNanos, physicalDisplayId, overrides);
    }
}
