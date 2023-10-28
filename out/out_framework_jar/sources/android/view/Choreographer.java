package android.view;

import android.graphics.FrameInfo;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.TimeUtils;
import android.view.DisplayEventReceiver;
import android.view.animation.AnimationUtils;
import com.mediatek.boostfwk.BoostFwkFactory;
import com.mediatek.boostfwk.scenario.frame.FrameScenario;
import com.mediatek.view.ViewDebugManager;
import com.transsion.hubcore.view.ITranChoreographer;
import java.io.PrintWriter;
/* loaded from: classes3.dex */
public final class Choreographer {
    public static final int CALLBACK_ANIMATION = 1;
    public static final int CALLBACK_COMMIT = 4;
    public static final int CALLBACK_INPUT = 0;
    public static final int CALLBACK_INSETS_ANIMATION = 2;
    private static final int CALLBACK_LAST = 4;
    public static final int CALLBACK_TRAVERSAL = 3;
    private static final long DEFAULT_FRAME_DELAY = 10;
    private static final int MSG_DO_FRAME = 0;
    private static final int MSG_DO_SCHEDULE_CALLBACK = 2;
    private static final int MSG_DO_SCHEDULE_VSYNC = 1;
    private static final String TAG = "Choreographer";
    private static volatile Choreographer mMainInstance;
    private CallbackRecord mCallbackPool;
    private final CallbackQueue[] mCallbackQueues;
    private boolean mCallbacksRunning;
    private boolean mDebugPrintNextFrameTimeDelta;
    private final FrameDisplayEventReceiver mDisplayEventReceiver;
    private int mFPSDivisor;
    FrameInfo mFrameInfo;
    @Deprecated
    private long mFrameIntervalNanos;
    private FrameScenario mFrameScenario;
    private boolean mFrameScheduled;
    private final FrameHandler mHandler;
    private long mLastFrameIntervalNanos;
    private long mLastFrameTimeNanos;
    private DisplayEventReceiver.VsyncEventData mLastVsyncEventData;
    private final Object mLock;
    private final Looper mLooper;
    private FrameData mPreAnimFrameData;
    private static final boolean DEBUG_JANK = ViewDebugManager.DEBUG_CHOREOGRAPHER_JANK;
    private static final boolean DEBUG_FRAMES = ViewDebugManager.DEBUG_CHOREOGRAPHER_FRAMES;
    private static volatile long sFrameDelay = 10;
    private static final ThreadLocal<Choreographer> sThreadInstance = new ThreadLocal<Choreographer>() { // from class: android.view.Choreographer.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.lang.ThreadLocal
        public Choreographer initialValue() {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalStateException("The current thread must have a looper!");
            }
            Choreographer choreographer = new Choreographer(looper, 0);
            if (looper == Looper.getMainLooper()) {
                Choreographer.mMainInstance = choreographer;
            }
            return choreographer;
        }
    };
    private static final ThreadLocal<Choreographer> sSfThreadInstance = new ThreadLocal<Choreographer>() { // from class: android.view.Choreographer.2
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.lang.ThreadLocal
        public Choreographer initialValue() {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalStateException("The current thread must have a looper!");
            }
            return new Choreographer(looper, 1);
        }
    };
    private static final boolean USE_VSYNC = SystemProperties.getBoolean("debug.choreographer.vsync", true);
    private static final boolean USE_FRAME_TIME = SystemProperties.getBoolean("debug.choreographer.frametime", true);
    private static final int SKIPPED_FRAME_WARNING_LIMIT = SystemProperties.getInt("debug.choreographer.skipwarning", 30);
    private static final Object FRAME_CALLBACK_TOKEN = new Object() { // from class: android.view.Choreographer.3
        public String toString() {
            return "FRAME_CALLBACK_TOKEN";
        }
    };
    private static final Object VSYNC_CALLBACK_TOKEN = new Object() { // from class: android.view.Choreographer.4
        public String toString() {
            return "VSYNC_CALLBACK_TOKEN";
        }
    };
    private static final String[] CALLBACK_TRACE_TITLES = {"input", "animation", "insets_animation", "traversal", "commit"};

    /* loaded from: classes3.dex */
    public interface FrameCallback {
        void doFrame(long j);
    }

    /* loaded from: classes3.dex */
    public interface VsyncCallback {
        void onVsync(FrameData frameData);
    }

    private Choreographer(Looper looper, int vsyncSource) {
        FrameDisplayEventReceiver frameDisplayEventReceiver;
        this.mFrameScenario = null;
        this.mLock = new Object();
        this.mFPSDivisor = 1;
        this.mLastVsyncEventData = new DisplayEventReceiver.VsyncEventData();
        this.mFrameInfo = new FrameInfo();
        this.mLooper = looper;
        this.mHandler = new FrameHandler(looper);
        if (USE_VSYNC) {
            frameDisplayEventReceiver = new FrameDisplayEventReceiver(looper, vsyncSource);
        } else {
            frameDisplayEventReceiver = null;
        }
        this.mDisplayEventReceiver = frameDisplayEventReceiver;
        this.mLastFrameTimeNanos = Long.MIN_VALUE;
        this.mFrameIntervalNanos = 1.0E9f / getRefreshRate();
        this.mCallbackQueues = new CallbackQueue[5];
        for (int i = 0; i <= 4; i++) {
            this.mCallbackQueues[i] = new CallbackQueue();
        }
        setFPSDivisor(SystemProperties.getInt(ThreadedRenderer.DEBUG_FPS_DIVISOR, 1));
    }

    private static float getRefreshRate() {
        DisplayInfo di = DisplayManagerGlobal.getInstance().getDisplayInfo(0);
        return di.getRefreshRate();
    }

    public static Choreographer getInstance() {
        return sThreadInstance.get();
    }

    public static Choreographer getSfInstance() {
        return sSfThreadInstance.get();
    }

    public static Choreographer getMainThreadInstance() {
        return mMainInstance;
    }

    public static void releaseInstance() {
        ThreadLocal<Choreographer> threadLocal = sThreadInstance;
        Choreographer old = threadLocal.get();
        threadLocal.remove();
        old.dispose();
    }

    private void dispose() {
        this.mDisplayEventReceiver.dispose();
    }

    public static long getFrameDelay() {
        return sFrameDelay;
    }

    public static void setFrameDelay(long frameDelay) {
        sFrameDelay = frameDelay;
    }

    public static long subtractFrameDelay(long delayMillis) {
        long frameDelay = sFrameDelay;
        if (delayMillis <= frameDelay) {
            return 0L;
        }
        return delayMillis - frameDelay;
    }

    public long getFrameIntervalNanos() {
        long j;
        synchronized (this.mLock) {
            j = this.mLastFrameIntervalNanos;
        }
        return j;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.println("Choreographer:");
        writer.print(innerPrefix);
        writer.print("mFrameScheduled=");
        writer.println(this.mFrameScheduled);
        writer.print(innerPrefix);
        writer.print("mLastFrameTime=");
        writer.println(TimeUtils.formatUptime(this.mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS));
    }

    public void postCallback(int callbackType, Runnable action, Object token) {
        postCallbackDelayed(callbackType, action, token, 0L);
    }

    public void postCallbackDelayed(int callbackType, Runnable action, Object token, long delayMillis) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (callbackType < 0 || callbackType > 4) {
            throw new IllegalArgumentException("callbackType is invalid");
        }
        postCallbackDelayedInternal(callbackType, action, token, delayMillis);
    }

    private void postCallbackDelayedInternal(int callbackType, Object action, Object token, long delayMillis) {
        if (DEBUG_FRAMES) {
            Log.d(TAG, "PostCallback: type=" + callbackType + ", action=" + action + ", token=" + token + ", delayMillis=" + delayMillis, new Throwable());
        }
        synchronized (this.mLock) {
            long now = SystemClock.uptimeMillis();
            long dueTime = now + delayMillis;
            this.mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);
            if (dueTime <= now) {
                scheduleFrameLocked(now);
            } else {
                Message msg = this.mHandler.obtainMessage(2, action);
                msg.arg1 = callbackType;
                msg.setAsynchronous(true);
                this.mHandler.sendMessageAtTime(msg, dueTime);
            }
        }
    }

    public void postVsyncCallback(VsyncCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        postCallbackDelayedInternal(1, callback, VSYNC_CALLBACK_TOKEN, 0L);
    }

    public void removeCallbacks(int callbackType, Runnable action, Object token) {
        if (callbackType < 0 || callbackType > 4) {
            throw new IllegalArgumentException("callbackType is invalid");
        }
        removeCallbacksInternal(callbackType, action, token);
    }

    private void removeCallbacksInternal(int callbackType, Object action, Object token) {
        if (DEBUG_FRAMES) {
            Log.d(TAG, "RemoveCallbacks: type=" + callbackType + ", action=" + action + ", token=" + token);
        }
        synchronized (this.mLock) {
            this.mCallbackQueues[callbackType].removeCallbacksLocked(action, token);
            if (action != null && token == null) {
                this.mHandler.removeMessages(2, action);
            }
        }
    }

    public void postFrameCallback(FrameCallback callback) {
        postFrameCallbackDelayed(callback, 0L);
    }

    public void postFrameCallbackDelayed(FrameCallback callback, long delayMillis) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        postCallbackDelayedInternal(1, callback, FRAME_CALLBACK_TOKEN, delayMillis);
    }

    public void removeFrameCallback(FrameCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        removeCallbacksInternal(1, callback, FRAME_CALLBACK_TOKEN);
    }

    public void removeVsyncCallback(VsyncCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        removeCallbacksInternal(1, callback, VSYNC_CALLBACK_TOKEN);
    }

    public long getFrameTime() {
        return getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;
    }

    public long getFrameTimeNanos() {
        long nanoTime;
        synchronized (this.mLock) {
            if (!this.mCallbacksRunning) {
                throw new IllegalStateException("This method must only be called as part of a callback while a frame is in progress.");
            }
            nanoTime = USE_FRAME_TIME ? this.mLastFrameTimeNanos : System.nanoTime();
        }
        return nanoTime;
    }

    public long getLastFrameTimeNanos() {
        long nanoTime;
        synchronized (this.mLock) {
            nanoTime = USE_FRAME_TIME ? this.mLastFrameTimeNanos : System.nanoTime();
        }
        return nanoTime;
    }

    private void scheduleFrameLocked(long now) {
        if (!this.mFrameScheduled) {
            this.mFrameScheduled = true;
            if (USE_VSYNC) {
                if (DEBUG_FRAMES) {
                    Log.d(TAG, "Scheduling next frame on vsync.");
                }
                if (!isRunningOnLooperThreadLocked()) {
                    Message msg = this.mHandler.obtainMessage(1);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageAtFrontOfQueue(msg);
                    return;
                }
                scheduleVsyncLocked();
                return;
            }
            long nextFrameTime = Math.max((this.mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS) + sFrameDelay, now);
            if (DEBUG_FRAMES) {
                Log.d(TAG, "Scheduling next frame in " + (nextFrameTime - now) + " ms.");
            }
            Message msg2 = this.mHandler.obtainMessage(0);
            msg2.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(msg2, nextFrameTime);
        }
    }

    public long getVsyncId() {
        return this.mLastVsyncEventData.preferredFrameTimeline().vsyncId;
    }

    public long getFrameDeadline() {
        return this.mLastVsyncEventData.preferredFrameTimeline().deadline;
    }

    void setFPSDivisor(int divisor) {
        if (divisor <= 0) {
            divisor = 1;
        }
        this.mFPSDivisor = divisor;
        ThreadedRenderer.setFPSDivisor(divisor);
    }

    private void traceMessage(String msg) {
        Trace.traceBegin(8L, msg);
        Trace.traceEnd(8L);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [848=13, 882=9, 883=5, 885=10, 886=5, 887=5, 888=5, 889=5, 890=10, 891=5, 892=5] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:242:0x0406 */
    /* JADX WARN: Can't wrap try/catch for region: R(14:23|24|(5:28|29|30|31|32)|57|(6:58|59|60|(5:62|(4:64|65|66|67)(5:243|244|245|(2:266|267)|(8:249|250|251|252|253|254|255|256)(1:248))|68|69|70)(1:274)|72|73)|(3:199|200|(22:202|203|204|205|206|207|208|209|210|211|212|213|214|(3:216|217|218)(1:220)|219|76|77|78|(6:(1:81)|85|86|87|88|(1:94)(2:92|93))(31:99|100|101|102|(2:104|(1:122)(5:108|109|110|111|(1:117)(2:115|116)))(1:192)|123|124|125|126|127|128|129|130|131|132|133|134|135|136|137|138|139|140|141|142|(3:158|159|(2:161|162)(1:163))(1:144)|145|146|(1:150)|151|(2:153|154)(2:155|156))|38|(3:39|40|41)|42))|75|76|77|78|(0)(0)|38|(3:39|40|41)|42) */
    /* JADX WARN: Code restructure failed: missing block: B:201:0x0569, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:202:0x056a, code lost:
        r29 = r8;
        r2 = 1;
        r13 = 4;
        r14 = 0;
        r34 = 8;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:112:0x02a4  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x0325  */
    /* JADX WARN: Removed duplicated region for block: B:220:0x05de  */
    /* JADX WARN: Type inference failed for: r0v32, types: [android.graphics.FrameInfo] */
    /* JADX WARN: Type inference failed for: r13v27, types: [long] */
    /* JADX WARN: Type inference failed for: r13v31 */
    /* JADX WARN: Type inference failed for: r13v33 */
    /* JADX WARN: Type inference failed for: r13v34 */
    /* JADX WARN: Type inference failed for: r13v35, types: [int] */
    /* JADX WARN: Type inference failed for: r13v38 */
    /* JADX WARN: Type inference failed for: r1v18, types: [com.mediatek.boostfwk.scenario.frame.FrameScenario] */
    /* JADX WARN: Type inference failed for: r38v0, types: [android.view.Choreographer] */
    /* JADX WARN: Type inference failed for: r5v32, types: [boolean] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void doFrame(long frameTimeNanos, int frame, DisplayEventReceiver.VsyncEventData vsyncEventData) {
        int i;
        long j;
        int i2;
        char c;
        Object obj;
        char c2;
        long frameTimeNanos2;
        long startNanos;
        long frameIntervalNanos;
        FrameData frameData;
        FrameData frameData2;
        long j2;
        int i3;
        long j3;
        char c3;
        int i4;
        long frameIntervalNanos2;
        long frameTimeNanos3;
        FrameData frameData3;
        long lastFrameOffset;
        long frameIntervalNanos3 = vsyncEventData.frameInterval;
        try {
            if (Trace.isTagEnabled(8L)) {
                try {
                    Trace.traceBegin(8L, "Choreographer#doFrame " + vsyncEventData.preferredFrameTimeline().vsyncId);
                } catch (Throwable th) {
                    th = th;
                    j = 8;
                    i2 = 0;
                    c = 4;
                    i = 1;
                    AnimationUtils.unlockAnimationClock();
                    Trace.traceEnd(j);
                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(i2).setBoostStatus(i).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                    if (this.mFrameScenario.isSFPEnable()) {
                        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(c).setBoostStatus(i));
                    }
                    throw th;
                }
            }
            FrameData frameData4 = new FrameData(frameTimeNanos, vsyncEventData);
            Object obj2 = this.mLock;
            synchronized (obj2) {
                try {
                    try {
                    } catch (Throwable th2) {
                        th = th2;
                        obj = obj2;
                        j = 8;
                        i2 = 0;
                        c2 = 4;
                        frameTimeNanos2 = frameTimeNanos;
                        i = 1;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    obj = obj2;
                    i = 1;
                    j = 8;
                    i2 = 0;
                    c2 = 4;
                }
                if (!this.mFrameScheduled) {
                    traceMessage("Frame not scheduled");
                    AnimationUtils.unlockAnimationClock();
                    Trace.traceEnd(8L);
                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(0).setBoostStatus(1).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                    if (this.mFrameScenario.isSFPEnable() && this.mFrameScenario.isFling()) {
                        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(4).setBoostStatus(1));
                        return;
                    }
                    return;
                }
                boolean z = DEBUG_JANK;
                if (z && this.mDebugPrintNextFrameTimeDelta) {
                    this.mDebugPrintNextFrameTimeDelta = false;
                    try {
                        try {
                            Log.d(TAG, "Frame time delta: " + (((float) (frameTimeNanos - this.mLastFrameTimeNanos)) * 1.0E-6f) + " ms");
                        } catch (Throwable th4) {
                            th = th4;
                            c2 = 4;
                            obj = obj2;
                            i2 = 0;
                            j = 8;
                            frameTimeNanos2 = frameTimeNanos;
                            i = 1;
                            while (true) {
                                try {
                                    try {
                                        break;
                                    } catch (Throwable th5) {
                                        th = th5;
                                        c = c2;
                                        AnimationUtils.unlockAnimationClock();
                                        Trace.traceEnd(j);
                                        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(i2).setBoostStatus(i).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                                        if (this.mFrameScenario.isSFPEnable() && this.mFrameScenario.isFling()) {
                                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(c).setBoostStatus(i));
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        obj = obj2;
                        j = 8;
                        i2 = 0;
                        c2 = 4;
                        frameTimeNanos2 = frameTimeNanos;
                        i = 1;
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                try {
                    startNanos = System.nanoTime();
                    long jitterNanos = startNanos - frameTimeNanos;
                    if (jitterNanos >= frameIntervalNanos3) {
                        if (frameIntervalNanos3 == 0) {
                            try {
                                Log.i(TAG, "Vsync data empty due to timeout");
                                frameIntervalNanos = frameIntervalNanos3;
                                frameData3 = frameData4;
                                lastFrameOffset = 0;
                            } catch (Throwable th8) {
                                th = th8;
                                c2 = 4;
                                obj = obj2;
                                i2 = 0;
                                j = 8;
                                frameTimeNanos2 = frameTimeNanos;
                                i = 1;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        } else {
                            try {
                                long lastFrameOffset2 = jitterNanos % frameIntervalNanos3;
                                long lastFrameOffset3 = jitterNanos / frameIntervalNanos3;
                                if (lastFrameOffset3 >= SKIPPED_FRAME_WARNING_LIMIT) {
                                    try {
                                        Log.i(TAG, "Skipped " + lastFrameOffset3 + " frames!  The application may be doing too much work on its main thread.");
                                    } catch (Throwable th9) {
                                        th = th9;
                                        frameTimeNanos2 = frameTimeNanos;
                                        obj = obj2;
                                        i = 1;
                                        c2 = 4;
                                        i2 = 0;
                                        j = 8;
                                    }
                                }
                                if (z) {
                                    try {
                                        frameIntervalNanos = frameIntervalNanos3;
                                        try {
                                            frameData3 = frameData4;
                                            lastFrameOffset = lastFrameOffset2;
                                            try {
                                                Log.d(TAG, "Missed vsync by " + (((float) jitterNanos) * 1.0E-6f) + " ms which is more than the frame interval of " + (((float) frameIntervalNanos) * 1.0E-6f) + " ms!  Skipping " + lastFrameOffset3 + " frames and setting frame time to " + (((float) lastFrameOffset) * 1.0E-6f) + " ms in the past.");
                                            } catch (Throwable th10) {
                                                th = th10;
                                                frameTimeNanos2 = frameTimeNanos;
                                                obj = obj2;
                                                i = 1;
                                                c2 = 4;
                                                i2 = 0;
                                                j = 8;
                                            }
                                        } catch (Throwable th11) {
                                            th = th11;
                                            frameTimeNanos2 = frameTimeNanos;
                                            obj = obj2;
                                            i = 1;
                                            c2 = 4;
                                            i2 = 0;
                                            j = 8;
                                        }
                                    } catch (Throwable th12) {
                                        th = th12;
                                        frameTimeNanos2 = frameTimeNanos;
                                        obj = obj2;
                                        i = 1;
                                        c2 = 4;
                                        i2 = 0;
                                        j = 8;
                                    }
                                } else {
                                    frameData3 = frameData4;
                                    frameIntervalNanos = frameIntervalNanos3;
                                    lastFrameOffset = lastFrameOffset2;
                                }
                            } catch (Throwable th13) {
                                th = th13;
                                c2 = 4;
                                obj = obj2;
                                i = 1;
                                i2 = 0;
                                j = 8;
                                frameTimeNanos2 = frameTimeNanos;
                            }
                        }
                        frameTimeNanos2 = startNanos - lastFrameOffset;
                        frameData = frameData3;
                        try {
                            frameData.updateFrameData(frameTimeNanos2);
                        } catch (Throwable th14) {
                            th = th14;
                            obj = obj2;
                            i = 1;
                            c2 = 4;
                            i2 = 0;
                            j = 8;
                        }
                    } else {
                        frameIntervalNanos = frameIntervalNanos3;
                        frameData = frameData4;
                        frameTimeNanos2 = frameTimeNanos;
                    }
                    try {
                    } catch (Throwable th15) {
                        th = th15;
                        obj = obj2;
                        i = 1;
                        c2 = 4;
                        i2 = 0;
                        j = 8;
                    }
                } catch (Throwable th16) {
                    th = th16;
                    c2 = 4;
                    obj = obj2;
                    i2 = 0;
                    i = 1;
                    j = 8;
                    frameTimeNanos2 = frameTimeNanos;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
                if (this.mFrameScenario.isSFPEnable()) {
                    try {
                    } catch (Throwable th17) {
                        th = th17;
                        obj = obj2;
                        i = 1;
                        c2 = 4;
                    }
                    if (this.mFrameScenario.isFling()) {
                        try {
                            try {
                                FrameData frameData5 = frameData;
                                try {
                                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(4).setChoreographer(this).setFrameData(frameData).setOrigFrameTimeNano(frameTimeNanos2).setBoostStatus(0).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                                    long updateFrameTimeNanos = this.mFrameScenario.getFrameTimeResult();
                                    if (updateFrameTimeNanos > 0) {
                                        frameTimeNanos2 = updateFrameTimeNanos;
                                        frameData2 = frameData5;
                                        try {
                                            frameData2.updateFrameData(frameTimeNanos2);
                                        } catch (Throwable th18) {
                                            th = th18;
                                            obj = obj2;
                                            i = 1;
                                            c2 = 4;
                                            i2 = 0;
                                            j = 8;
                                        }
                                    } else {
                                        frameData2 = frameData5;
                                    }
                                    this.mPreAnimFrameData = frameData2;
                                    j2 = this.mLastFrameTimeNanos;
                                    if (frameTimeNanos2 >= j2) {
                                        if (z) {
                                            Log.d(TAG, "Frame time appears to be going backwards.  May be due to a previously skipped frame.  Waiting for next vsync.");
                                        }
                                        try {
                                            traceMessage("Frame time goes backward");
                                            scheduleVsyncLocked();
                                            AnimationUtils.unlockAnimationClock();
                                            Trace.traceEnd(8L);
                                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(0).setBoostStatus(1).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                                            if (this.mFrameScenario.isSFPEnable() && this.mFrameScenario.isFling()) {
                                                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(4).setBoostStatus(1));
                                                return;
                                            }
                                            return;
                                        } catch (Throwable th19) {
                                            th = th19;
                                            obj = obj2;
                                            i = 1;
                                            c2 = 4;
                                            i2 = 0;
                                            j = 8;
                                        }
                                    } else {
                                        FrameData frameData6 = frameData2;
                                        try {
                                            int i5 = this.mFPSDivisor;
                                            if (i5 > 1) {
                                                long timeSinceVsync = frameTimeNanos2 - j2;
                                                if (timeSinceVsync >= i5 * frameIntervalNanos || timeSinceVsync <= 0) {
                                                    j3 = 8;
                                                    c3 = 4;
                                                    i3 = 1;
                                                    i4 = 0;
                                                } else {
                                                    try {
                                                        traceMessage("Frame skipped due to FPSDivisor");
                                                        scheduleVsyncLocked();
                                                        AnimationUtils.unlockAnimationClock();
                                                        Trace.traceEnd(8L);
                                                        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(0).setBoostStatus(1).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                                                        if (this.mFrameScenario.isSFPEnable() && this.mFrameScenario.isFling()) {
                                                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(4).setBoostStatus(1));
                                                            return;
                                                        }
                                                        return;
                                                    } catch (Throwable th20) {
                                                        th = th20;
                                                        obj = obj2;
                                                        i = 1;
                                                        c2 = 4;
                                                        i2 = 0;
                                                        j = 8;
                                                    }
                                                }
                                            } else {
                                                i3 = 1;
                                                j3 = 8;
                                                c3 = 4;
                                                i4 = 0;
                                            }
                                            try {
                                                ?? r0 = this.mFrameInfo;
                                                try {
                                                    long j4 = vsyncEventData.preferredFrameTimeline().vsyncId;
                                                    try {
                                                        long frameIntervalNanos4 = frameIntervalNanos;
                                                        try {
                                                            long frameIntervalNanos5 = vsyncEventData.preferredFrameTimeline().deadline;
                                                            long intendedFrameTimeNanos = vsyncEventData.frameInterval;
                                                            obj = obj2;
                                                            j = 8;
                                                            ?? r13 = j4;
                                                            ?? r5 = i4;
                                                            try {
                                                                r0.setVsync(frameTimeNanos, frameTimeNanos2, r13, frameIntervalNanos5, startNanos, intendedFrameTimeNanos);
                                                                this.mFrameScheduled = r5;
                                                                this.mLastFrameTimeNanos = frameTimeNanos2;
                                                                try {
                                                                    this.mLastFrameIntervalNanos = frameIntervalNanos4;
                                                                    this.mLastVsyncEventData = vsyncEventData;
                                                                    try {
                                                                        AnimationUtils.lockAnimationClock(frameTimeNanos2 / TimeUtils.NANOS_PER_MS);
                                                                        this.mFrameInfo.markInputHandlingStart();
                                                                        doCallbacks(r5 == true ? 1 : 0, frameData6, frameIntervalNanos4);
                                                                        try {
                                                                            if (this.mFrameScenario.isPreAnimEnable()) {
                                                                                try {
                                                                                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(5));
                                                                                    if (this.mFrameScenario.isPreAnim()) {
                                                                                        frameIntervalNanos2 = frameIntervalNanos4;
                                                                                        frameTimeNanos3 = frameTimeNanos2;
                                                                                        i2 = r5 == true ? 1 : 0;
                                                                                        r13 = 4;
                                                                                    } else {
                                                                                        frameIntervalNanos2 = frameIntervalNanos4;
                                                                                        frameTimeNanos3 = frameTimeNanos2;
                                                                                        r13 = 4;
                                                                                        i2 = r5 == true ? 1 : 0;
                                                                                        markAnimations(frameIntervalNanos2, startNanos, frameData6);
                                                                                    }
                                                                                } catch (Throwable th21) {
                                                                                    th = th21;
                                                                                    i2 = r5 == true ? 1 : 0;
                                                                                    c = 4;
                                                                                    i = 1;
                                                                                    AnimationUtils.unlockAnimationClock();
                                                                                    Trace.traceEnd(j);
                                                                                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(i2).setBoostStatus(i).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                                                                                    if (this.mFrameScenario.isSFPEnable()) {
                                                                                    }
                                                                                    throw th;
                                                                                }
                                                                            } else {
                                                                                frameIntervalNanos2 = frameIntervalNanos4;
                                                                                frameTimeNanos3 = frameTimeNanos2;
                                                                                i2 = r5 == true ? 1 : 0;
                                                                                r13 = 4;
                                                                                markAnimations(frameIntervalNanos2, startNanos, frameData6);
                                                                            }
                                                                            doCallbacks(2, frameData6, frameIntervalNanos2);
                                                                            this.mFrameInfo.markPerformTraversalsStart();
                                                                            doCallbacks(3, frameData6, frameIntervalNanos2);
                                                                            doCallbacks(r13, frameData6, frameIntervalNanos2);
                                                                            AnimationUtils.unlockAnimationClock();
                                                                            Trace.traceEnd(8L);
                                                                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(i2).setBoostStatus(1).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                                                                            if (this.mFrameScenario.isSFPEnable() && this.mFrameScenario.isFling()) {
                                                                                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(r13).setBoostStatus(1));
                                                                            }
                                                                            if (DEBUG_FRAMES) {
                                                                                long endNanos = System.nanoTime();
                                                                                Log.d(TAG, "Frame " + frame + ": Finished, took " + (((float) (endNanos - startNanos)) * 1.0E-6f) + " ms, latency " + (((float) (startNanos - frameTimeNanos3)) * 1.0E-6f) + " ms.");
                                                                                return;
                                                                            }
                                                                            return;
                                                                        } catch (Throwable th22) {
                                                                            th = th22;
                                                                            i = 1;
                                                                            c = r13;
                                                                        }
                                                                    } catch (Throwable th23) {
                                                                        th = th23;
                                                                        i2 = r5 == true ? 1 : 0;
                                                                        c = 4;
                                                                        i = 1;
                                                                    }
                                                                } catch (Throwable th24) {
                                                                    th = th24;
                                                                    i2 = r5 == true ? 1 : 0;
                                                                    c2 = 4;
                                                                    i = 1;
                                                                }
                                                            } catch (Throwable th25) {
                                                                th = th25;
                                                                i2 = r5 == true ? 1 : 0;
                                                                c2 = 4;
                                                                i = 1;
                                                            }
                                                        } catch (Throwable th26) {
                                                            th = th26;
                                                            obj = obj2;
                                                            i = i3;
                                                            i2 = i4;
                                                            c2 = 4;
                                                            j = 8;
                                                        }
                                                    } catch (Throwable th27) {
                                                        th = th27;
                                                        obj = obj2;
                                                        i = i3;
                                                        i2 = i4;
                                                        c2 = 4;
                                                        j = 8;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } catch (Throwable th28) {
                                                    th = th28;
                                                    obj = obj2;
                                                    c2 = c3;
                                                    i = i3;
                                                    i2 = i4;
                                                }
                                            } catch (Throwable th29) {
                                                th = th29;
                                                j = j3;
                                                obj = obj2;
                                                c2 = c3;
                                                i = i3;
                                                i2 = i4;
                                            }
                                        } catch (Throwable th30) {
                                            th = th30;
                                            obj = obj2;
                                            i = 1;
                                            c2 = 4;
                                            i2 = 0;
                                            j = 8;
                                        }
                                    }
                                } catch (Throwable th31) {
                                    th = th31;
                                    obj = obj2;
                                    i = 1;
                                    c2 = 4;
                                    i2 = 0;
                                    j = 8;
                                }
                            } catch (Throwable th32) {
                                th = th32;
                                obj = obj2;
                                i2 = 0;
                                i = 1;
                                c2 = 4;
                                j = 8;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        } catch (Throwable th33) {
                            th = th33;
                            obj = obj2;
                            c2 = 4;
                            i = 1;
                            i2 = 0;
                            j = 8;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                        while (true) {
                            break;
                            break;
                        }
                        throw th;
                    }
                }
                frameData2 = frameData;
                j2 = this.mLastFrameTimeNanos;
                if (frameTimeNanos2 >= j2) {
                }
                while (true) {
                    break;
                    break;
                }
                throw th;
            }
        } catch (Throwable th34) {
            th = th34;
            i = 1;
            j = 8;
            i2 = 0;
            c = 4;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [974=4, 977=4] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX WARN: Removed duplicated region for block: B:32:0x00af A[Catch: all -> 0x0152, TryCatch #1 {all -> 0x0152, blocks: (B:30:0x009e, B:32:0x00af, B:35:0x00cc, B:37:0x00d0, B:38:0x0111), top: B:78:0x009e }] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x00cc A[Catch: all -> 0x0152, TryCatch #1 {all -> 0x0152, blocks: (B:30:0x009e, B:32:0x00af, B:35:0x00cc, B:37:0x00d0, B:38:0x0111), top: B:78:0x009e }] */
    /* JADX WARN: Removed duplicated region for block: B:85:0x011b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void doCallbacks(int callbackType, FrameData frameData, long frameIntervalNanos) {
        long frameTimeNanos;
        CallbackRecord c;
        long frameTimeNanos2 = frameData.mFrameTimeNanos;
        synchronized (this.mLock) {
            try {
                try {
                    long now = System.nanoTime();
                    CallbackRecord callbacks = this.mCallbackQueues[callbackType].extractDueCallbacksLocked(now / TimeUtils.NANOS_PER_MS);
                    if (callbacks == null) {
                        return;
                    }
                    this.mCallbacksRunning = true;
                    try {
                        if (callbackType == 4) {
                            long jitterNanos = now - frameTimeNanos2;
                            frameTimeNanos = frameTimeNanos2;
                            try {
                                Trace.traceCounter(8L, "jitterNanos", (int) jitterNanos);
                                if (jitterNanos >= 2 * frameIntervalNanos) {
                                    long lastFrameOffset = (jitterNanos % frameIntervalNanos) + frameIntervalNanos;
                                    if (DEBUG_JANK) {
                                        Log.d(TAG, "Commit callback delayed by " + (((float) jitterNanos) * 1.0E-6f) + " ms which is more than twice the frame interval of " + (((float) frameIntervalNanos) * 1.0E-6f) + " ms!  Setting frame time to " + (((float) lastFrameOffset) * 1.0E-6f) + " ms in the past.");
                                        this.mDebugPrintNextFrameTimeDelta = true;
                                    }
                                    long frameTimeNanos3 = now - lastFrameOffset;
                                    try {
                                        this.mLastFrameTimeNanos = frameTimeNanos3;
                                        frameData.updateFrameData(frameTimeNanos3);
                                        Trace.traceBegin(8L, CALLBACK_TRACE_TITLES[callbackType]);
                                        if (this.mFrameScenario.isListenFrameHint()) {
                                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(1).setBoostStatus(0).setFrameStep(callbackType));
                                        }
                                        for (c = callbacks; c != null; c = c.next) {
                                            if (DEBUG_FRAMES) {
                                                Log.d(TAG, "RunCallback: type=" + callbackType + ", action=" + c.action + ", token=" + c.token + ", latencyMillis=" + (SystemClock.uptimeMillis() - c.dueTime));
                                            }
                                            c.run(frameData);
                                        }
                                        synchronized (this.mLock) {
                                            this.mCallbacksRunning = false;
                                            do {
                                                CallbackRecord next = callbacks.next;
                                                recycleCallbackLocked(callbacks);
                                                callbacks = next;
                                            } while (callbacks != null);
                                        }
                                        if (this.mFrameScenario.isListenFrameHint()) {
                                            BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(1).setBoostStatus(1).setFrameStep(callbackType));
                                        }
                                        Trace.traceEnd(8L);
                                        return;
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } else {
                            frameTimeNanos = frameTimeNanos2;
                        }
                        Trace.traceBegin(8L, CALLBACK_TRACE_TITLES[callbackType]);
                        if (this.mFrameScenario.isListenFrameHint()) {
                        }
                        while (c != null) {
                        }
                        synchronized (this.mLock) {
                        }
                    } catch (Throwable th3) {
                        synchronized (this.mLock) {
                            this.mCallbacksRunning = false;
                            while (true) {
                                CallbackRecord next2 = callbacks.next;
                                recycleCallbackLocked(callbacks);
                                callbacks = next2;
                                if (callbacks == null) {
                                    break;
                                }
                            }
                            if (this.mFrameScenario.isListenFrameHint()) {
                                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(1).setBoostStatus(1).setFrameStep(callbackType));
                            }
                            Trace.traceEnd(8L);
                            throw th3;
                        }
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    void doScheduleVsync() {
        synchronized (this.mLock) {
            if (this.mFrameScheduled) {
                scheduleVsyncLocked();
            }
        }
    }

    void doScheduleCallback(int callbackType) {
        synchronized (this.mLock) {
            if (!this.mFrameScheduled) {
                long now = SystemClock.uptimeMillis();
                if (this.mCallbackQueues[callbackType].hasDueCallbacksLocked(now)) {
                    scheduleFrameLocked(now);
                }
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1015=4] */
    private void scheduleVsyncLocked() {
        try {
            Trace.traceBegin(8L, "Choreographer#scheduleVsyncLocked");
            this.mDisplayEventReceiver.scheduleVsync();
        } finally {
            Trace.traceEnd(8L);
            FrameScenario frameScenario = this.mFrameScenario;
            if (frameScenario != null && frameScenario.isListenFrameHint()) {
                BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(this.mFrameScenario.setAction(2).setBoostStatus(1));
            }
        }
    }

    private boolean isRunningOnLooperThreadLocked() {
        return Looper.myLooper() == this.mLooper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public CallbackRecord obtainCallbackLocked(long dueTime, Object action, Object token) {
        CallbackRecord callback = this.mCallbackPool;
        if (callback == null) {
            callback = new CallbackRecord();
        } else {
            this.mCallbackPool = callback.next;
            callback.next = null;
        }
        callback.dueTime = dueTime;
        callback.action = action;
        callback.token = token;
        return callback;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recycleCallbackLocked(CallbackRecord callback) {
        callback.action = null;
        callback.token = null;
        callback.next = this.mCallbackPool;
        this.mCallbackPool = callback;
    }

    /* loaded from: classes3.dex */
    public static class FrameTimeline {
        static final FrameTimeline INVALID_FRAME_TIMELINE = new FrameTimeline(-1, Long.MAX_VALUE, Long.MAX_VALUE);
        private long mDeadlineNanos;
        private long mExpectedPresentTimeNanos;
        private long mVsyncId;

        FrameTimeline(long vsyncId, long expectedPresentTimeNanos, long deadlineNanos) {
            this.mVsyncId = vsyncId;
            this.mExpectedPresentTimeNanos = expectedPresentTimeNanos;
            this.mDeadlineNanos = deadlineNanos;
        }

        public long getVsyncId() {
            return this.mVsyncId;
        }

        void resetVsyncId() {
            this.mVsyncId = -1L;
        }

        public long getExpectedPresentationTimeNanos() {
            return this.mExpectedPresentTimeNanos;
        }

        public long getDeadlineNanos() {
            return this.mDeadlineNanos;
        }
    }

    /* loaded from: classes3.dex */
    public static class FrameData {
        static final FrameTimeline[] INVALID_FRAME_TIMELINES = new FrameTimeline[0];
        private long mFrameTimeNanos;
        private FrameTimeline[] mFrameTimelines;
        private FrameTimeline mPreferredFrameTimeline;

        FrameData() {
            this.mFrameTimelines = INVALID_FRAME_TIMELINES;
            this.mPreferredFrameTimeline = FrameTimeline.INVALID_FRAME_TIMELINE;
        }

        FrameData(long frameTimeNanos, DisplayEventReceiver.VsyncEventData vsyncEventData) {
            FrameTimeline[] frameTimelines = new FrameTimeline[vsyncEventData.frameTimelines.length];
            for (int i = 0; i < vsyncEventData.frameTimelines.length; i++) {
                DisplayEventReceiver.VsyncEventData.FrameTimeline frameTimeline = vsyncEventData.frameTimelines[i];
                frameTimelines[i] = new FrameTimeline(frameTimeline.vsyncId, frameTimeline.expectedPresentTime, frameTimeline.deadline);
            }
            this.mFrameTimeNanos = frameTimeNanos;
            this.mFrameTimelines = frameTimelines;
            this.mPreferredFrameTimeline = frameTimelines[vsyncEventData.preferredFrameTimelineIndex];
        }

        void updateFrameData(long frameTimeNanos) {
            FrameTimeline[] frameTimelineArr;
            this.mFrameTimeNanos = frameTimeNanos;
            for (FrameTimeline ft : this.mFrameTimelines) {
                ft.resetVsyncId();
            }
        }

        public long getFrameTimeNanos() {
            return this.mFrameTimeNanos;
        }

        public FrameTimeline[] getFrameTimelines() {
            return this.mFrameTimelines;
        }

        public FrameTimeline getPreferredFrameTimeline() {
            return this.mPreferredFrameTimeline;
        }

        private FrameTimeline[] convertFrameTimelines(DisplayEventReceiver.VsyncEventData vsyncEventData) {
            FrameTimeline[] frameTimelines = new FrameTimeline[vsyncEventData.frameTimelines.length];
            for (int i = 0; i < vsyncEventData.frameTimelines.length; i++) {
                DisplayEventReceiver.VsyncEventData.FrameTimeline frameTimeline = vsyncEventData.frameTimelines[i];
                frameTimelines[i] = new FrameTimeline(frameTimeline.vsyncId, frameTimeline.expectedPresentTime, frameTimeline.deadline);
            }
            return frameTimelines;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public final class FrameHandler extends Handler {
        public FrameHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Choreographer.this.doFrame(System.nanoTime(), 0, new DisplayEventReceiver.VsyncEventData());
                    return;
                case 1:
                    Choreographer.this.doScheduleVsync();
                    return;
                case 2:
                    Choreographer.this.doScheduleCallback(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public final class FrameDisplayEventReceiver extends DisplayEventReceiver implements Runnable {
        private int mFrame;
        private boolean mHavePendingVsync;
        private DisplayEventReceiver.VsyncEventData mLastVsyncEventData;
        private long mTimestampNanos;

        public FrameDisplayEventReceiver(Looper looper, int vsyncSource) {
            super(looper, vsyncSource, 0);
            this.mLastVsyncEventData = new DisplayEventReceiver.VsyncEventData();
        }

        @Override // android.view.DisplayEventReceiver
        public void onVsync(long timestampNanos, long physicalDisplayId, int frame, DisplayEventReceiver.VsyncEventData vsyncEventData) {
            try {
                if (Trace.isTagEnabled(8L)) {
                    Trace.traceBegin(8L, "Choreographer#onVsync " + vsyncEventData.preferredFrameTimeline().vsyncId);
                }
                long now = System.nanoTime();
                if (timestampNanos - now > 0) {
                    Log.w(Choreographer.TAG, "Frame time is " + (((float) (timestampNanos - now)) * 1.0E-6f) + "  now = " + now + "  timestampNanos = " + timestampNanos + " ms in the future!  Check that graphics HAL is generating vsync timestamps using the correct timebase.");
                    timestampNanos = now;
                }
                if (this.mHavePendingVsync) {
                    Log.w(Choreographer.TAG, "Already have a pending vsync event.  There should only be one at a time.");
                } else {
                    this.mHavePendingVsync = true;
                }
                this.mTimestampNanos = timestampNanos;
                this.mFrame = frame;
                this.mLastVsyncEventData = vsyncEventData;
                Message msg = Message.obtain(Choreographer.this.mHandler, this);
                msg.setAsynchronous(true);
                Choreographer.this.mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
                if (Choreographer.this.mFrameScenario == null) {
                    Choreographer.this.mFrameScenario = new FrameScenario();
                }
                if (vsyncEventData != null) {
                    BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(Choreographer.this.mFrameScenario.setAction(0).setBoostStatus(0).setFrameId(vsyncEventData.preferredFrameTimeline().vsyncId));
                }
            } finally {
                Trace.traceEnd(8L);
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            this.mHavePendingVsync = false;
            Choreographer.this.doFrame(this.mTimestampNanos, this.mFrame, this.mLastVsyncEventData);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class CallbackRecord {
        public Object action;
        public long dueTime;
        public CallbackRecord next;
        public Object token;

        private CallbackRecord() {
        }

        public void run(long frameTimeNanos) {
            if (this.token == Choreographer.FRAME_CALLBACK_TOKEN) {
                ((FrameCallback) this.action).doFrame(frameTimeNanos);
            } else {
                ((Runnable) this.action).run();
            }
        }

        void run(FrameData frameData) {
            if (this.token == Choreographer.VSYNC_CALLBACK_TOKEN) {
                ((VsyncCallback) this.action).onVsync(frameData);
            } else {
                run(frameData.getFrameTimeNanos());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public final class CallbackQueue {
        private CallbackRecord mHead;

        private CallbackQueue() {
        }

        public boolean hasDueCallbacksLocked(long now) {
            CallbackRecord callbackRecord = this.mHead;
            return callbackRecord != null && callbackRecord.dueTime <= now;
        }

        public CallbackRecord extractDueCallbacksLocked(long now) {
            CallbackRecord callbacks = this.mHead;
            if (callbacks == null || callbacks.dueTime > now) {
                return null;
            }
            CallbackRecord last = callbacks;
            CallbackRecord next = last.next;
            while (true) {
                if (next == null) {
                    break;
                } else if (next.dueTime > now) {
                    last.next = null;
                    break;
                } else {
                    last = next;
                    next = next.next;
                }
            }
            this.mHead = next;
            return callbacks;
        }

        public void addCallbackLocked(long dueTime, Object action, Object token) {
            CallbackRecord callback = Choreographer.this.obtainCallbackLocked(dueTime, action, token);
            CallbackRecord entry = this.mHead;
            if (entry == null) {
                this.mHead = callback;
            } else if (dueTime < entry.dueTime) {
                callback.next = entry;
                this.mHead = callback;
            } else {
                while (true) {
                    if (entry.next == null) {
                        break;
                    } else if (dueTime < entry.next.dueTime) {
                        callback.next = entry.next;
                        break;
                    } else {
                        entry = entry.next;
                    }
                }
                entry.next = callback;
            }
        }

        public void removeCallbacksLocked(Object action, Object token) {
            CallbackRecord predecessor = null;
            CallbackRecord callback = this.mHead;
            while (callback != null) {
                CallbackRecord next = callback.next;
                if ((action == null || callback.action == action) && (token == null || callback.token == token)) {
                    if (predecessor != null) {
                        predecessor.next = next;
                    } else {
                        this.mHead = next;
                    }
                    Choreographer.this.recycleCallbackLocked(callback);
                } else {
                    predecessor = callback;
                }
                callback = next;
            }
        }
    }

    public void doEstimationFrame(long frameTimeNano) {
        try {
            doFrame(frameTimeNano, 0, new DisplayEventReceiver.VsyncEventData());
        } catch (RuntimeException e) {
            Log.w(TAG, "doEstimationFrame maybe error.");
        }
    }

    public void doPreAnimation(long frameTimeNanos, long frameIntervalNanos) {
        try {
            AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);
            this.mPreAnimFrameData.updateFrameData(frameTimeNanos);
            this.mFrameInfo.markAnimationsStart();
            doCallbacks(1, this.mPreAnimFrameData, frameIntervalNanos);
        } finally {
            AnimationUtils.unlockAnimationClock();
        }
    }

    private void markAnimations(long frameIntervalNanos, long startNanos, FrameData frameData) {
        this.mFrameInfo.markAnimationsStart();
        ITranChoreographer.Instance().animationBegin(frameIntervalNanos, startNanos);
        doCallbacks(1, frameData, frameIntervalNanos);
        ITranChoreographer.Instance().animationEnd();
    }

    public void forceFrameScheduled() {
        this.mFrameScheduled = true;
    }

    public boolean isEmptyCallback() {
        long now = SystemClock.uptimeMillis();
        for (int i = 0; i <= 4; i++) {
            if (this.mCallbackQueues[i].hasDueCallbacksLocked(now)) {
                return false;
            }
        }
        return true;
    }

    public void forceScheduleNexFrame() {
        scheduleFrameLocked(SystemClock.uptimeMillis());
    }
}
