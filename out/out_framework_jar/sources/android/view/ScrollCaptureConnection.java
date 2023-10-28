package android.view;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.Trace;
import android.telephony.BinderCacheManager$BinderDeathTracker$$ExternalSyntheticLambda0;
import android.util.CloseGuard;
import android.util.Log;
import android.view.IScrollCaptureConnection;
import java.lang.ref.Reference;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
public class ScrollCaptureConnection extends IScrollCaptureConnection.Stub implements IBinder.DeathRecipient {
    private static final String END_CAPTURE = "endCapture";
    private static final String REQUEST_IMAGE = "requestImage";
    private static final String SESSION = "Session";
    private static final String START_CAPTURE = "startCapture";
    private static final String TAG = "ScrollCaptureConnection";
    private static final String TRACE_TRACK = "Scroll Capture";
    private volatile boolean mActive;
    private CancellationSignal mCancellation;
    private volatile boolean mConnected;
    private ScrollCaptureCallback mLocal;
    private final Point mPositionInWindow;
    private IScrollCaptureCallbacks mRemote;
    private final Rect mScrollBounds;
    private ScrollCaptureSession mSession;
    private int mTraceId;
    private final Executor mUiThread;
    private final Object mLock = new Object();
    private final CloseGuard mCloseGuard = new CloseGuard();

    public ScrollCaptureConnection(Executor uiThread, ScrollCaptureTarget selectedTarget) {
        this.mUiThread = (Executor) Objects.requireNonNull(uiThread, "<uiThread> must non-null");
        Objects.requireNonNull(selectedTarget, "<selectedTarget> must non-null");
        this.mScrollBounds = (Rect) Objects.requireNonNull(Rect.copyOrNull(selectedTarget.getScrollBounds()), "target.getScrollBounds() must be non-null to construct a client");
        this.mLocal = selectedTarget.getCallback();
        this.mPositionInWindow = new Point(selectedTarget.getPositionInWindow());
    }

    @Override // android.view.IScrollCaptureConnection
    public ICancellationSignal startCapture(Surface surface, IScrollCaptureCallbacks remote) throws RemoteException {
        int identityHashCode = System.identityHashCode(surface);
        this.mTraceId = identityHashCode;
        Trace.asyncTraceForTrackBegin(2L, TRACE_TRACK, "Session", identityHashCode);
        Trace.asyncTraceForTrackBegin(2L, TRACE_TRACK, START_CAPTURE, this.mTraceId);
        this.mCloseGuard.open("ScrollCaptureConnection.close");
        if (!surface.isValid()) {
            throw new RemoteException(new IllegalArgumentException("surface must be valid"));
        }
        IScrollCaptureCallbacks iScrollCaptureCallbacks = (IScrollCaptureCallbacks) Objects.requireNonNull(remote, "<callbacks> must non-null");
        this.mRemote = iScrollCaptureCallbacks;
        iScrollCaptureCallbacks.asBinder().linkToDeath(this, 0);
        this.mConnected = true;
        ICancellationSignal cancellation = CancellationSignal.createTransport();
        synchronized (this.mLock) {
            this.mCancellation = CancellationSignal.fromTransport(cancellation);
        }
        this.mSession = new ScrollCaptureSession(surface, this.mScrollBounds, this.mPositionInWindow);
        final Runnable listener = SafeCallback.create(this.mCancellation, this.mUiThread, new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                ScrollCaptureConnection.this.onStartCaptureCompleted();
            }
        });
        final ScrollCaptureCallback callback = this.mLocal;
        this.mUiThread.execute(new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                ScrollCaptureConnection.this.m4869lambda$startCapture$0$androidviewScrollCaptureConnection(callback, listener);
            }
        });
        return cancellation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startCapture$0$android-view-ScrollCaptureConnection  reason: not valid java name */
    public /* synthetic */ void m4869lambda$startCapture$0$androidviewScrollCaptureConnection(ScrollCaptureCallback callback, Runnable listener) {
        if (callback != null) {
            callback.onScrollCaptureStart(this.mSession, this.mCancellation, listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onStartCaptureCompleted() {
        this.mActive = true;
        try {
            IScrollCaptureCallbacks iScrollCaptureCallbacks = this.mRemote;
            if (iScrollCaptureCallbacks != null) {
                iScrollCaptureCallbacks.onCaptureStarted();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Shutting down due to error: ", e);
            close();
        }
        synchronized (this.mLock) {
            this.mCancellation = null;
        }
        Trace.asyncTraceForTrackEnd(2L, TRACE_TRACK, START_CAPTURE, this.mTraceId);
    }

    @Override // android.view.IScrollCaptureConnection
    public ICancellationSignal requestImage(final Rect requestRect) throws RemoteException {
        CancellationSignal fromTransport;
        Trace.asyncTraceForTrackBegin(2L, TRACE_TRACK, REQUEST_IMAGE, this.mTraceId);
        checkActive();
        cancelPendingAction();
        ICancellationSignal cancellation = CancellationSignal.createTransport();
        synchronized (this.mLock) {
            fromTransport = CancellationSignal.fromTransport(cancellation);
            this.mCancellation = fromTransport;
        }
        final Consumer<Rect> listener = SafeCallback.create(fromTransport, this.mUiThread, new Consumer() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ScrollCaptureConnection.this.onImageRequestCompleted((Rect) obj);
            }
        });
        final ScrollCaptureCallback callback = this.mLocal;
        this.mUiThread.execute(new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ScrollCaptureConnection.this.m4868lambda$requestImage$1$androidviewScrollCaptureConnection(callback, requestRect, listener);
            }
        });
        return cancellation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestImage$1$android-view-ScrollCaptureConnection  reason: not valid java name */
    public /* synthetic */ void m4868lambda$requestImage$1$androidviewScrollCaptureConnection(ScrollCaptureCallback callback, Rect requestRect, Consumer listener) {
        if (callback != null) {
            callback.onScrollCaptureImageRequest(this.mSession, this.mCancellation, new Rect(requestRect), listener);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [205=4, 207=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void onImageRequestCompleted(Rect capturedArea) {
        try {
            try {
                IScrollCaptureCallbacks iScrollCaptureCallbacks = this.mRemote;
                if (iScrollCaptureCallbacks != null) {
                    iScrollCaptureCallbacks.onImageRequestCompleted(0, capturedArea);
                }
                synchronized (this.mLock) {
                    this.mCancellation = null;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Shutting down due to error: ", e);
                close();
                synchronized (this.mLock) {
                    this.mCancellation = null;
                }
            }
            Trace.asyncTraceForTrackEnd(2L, TRACE_TRACK, REQUEST_IMAGE, this.mTraceId);
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCancellation = null;
                throw th;
            }
        }
    }

    @Override // android.view.IScrollCaptureConnection
    public ICancellationSignal endCapture() throws RemoteException {
        CancellationSignal fromTransport;
        Trace.asyncTraceForTrackBegin(2L, TRACE_TRACK, END_CAPTURE, this.mTraceId);
        checkActive();
        cancelPendingAction();
        ICancellationSignal cancellation = CancellationSignal.createTransport();
        synchronized (this.mLock) {
            fromTransport = CancellationSignal.fromTransport(cancellation);
            this.mCancellation = fromTransport;
        }
        final Runnable listener = SafeCallback.create(fromTransport, this.mUiThread, new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ScrollCaptureConnection.this.onEndCaptureCompleted();
            }
        });
        final ScrollCaptureCallback callback = this.mLocal;
        this.mUiThread.execute(new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ScrollCaptureConnection.lambda$endCapture$2(ScrollCaptureCallback.this, listener);
            }
        });
        return cancellation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$endCapture$2(ScrollCaptureCallback callback, Runnable listener) {
        if (callback != null) {
            callback.onScrollCaptureEnd(listener);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [256=6] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: private */
    public void onEndCaptureCompleted() {
        this.mActive = false;
        try {
            try {
                IScrollCaptureCallbacks iScrollCaptureCallbacks = this.mRemote;
                if (iScrollCaptureCallbacks != null) {
                    iScrollCaptureCallbacks.onCaptureEnded();
                }
                synchronized (this.mLock) {
                    this.mCancellation = null;
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Caught exception confirming capture end!", e);
                synchronized (this.mLock) {
                    this.mCancellation = null;
                }
            }
            close();
            Trace.asyncTraceForTrackEnd(2L, TRACE_TRACK, END_CAPTURE, this.mTraceId);
            Trace.asyncTraceForTrackEnd(2L, TRACE_TRACK, "Session", this.mTraceId);
        } catch (Throwable th) {
            synchronized (this.mLock) {
                this.mCancellation = null;
                close();
                throw th;
            }
        }
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Trace.instantForTrack(2L, TRACE_TRACK, "binderDied");
        Log.e(TAG, "Controlling process just died.");
        close();
    }

    @Override // android.view.IScrollCaptureConnection
    public void close() {
        Trace.instantForTrack(2L, TRACE_TRACK, "close");
        if (this.mActive) {
            Log.w(TAG, "close(): capture session still active! Ending now.");
            cancelPendingAction();
            final ScrollCaptureCallback callback = this.mLocal;
            this.mUiThread.execute(new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ScrollCaptureCallback.this.onScrollCaptureEnd(new Runnable() { // from class: android.view.ScrollCaptureConnection$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            ScrollCaptureConnection.lambda$close$3();
                        }
                    });
                }
            });
            this.mActive = false;
        }
        IScrollCaptureCallbacks iScrollCaptureCallbacks = this.mRemote;
        if (iScrollCaptureCallbacks != null) {
            iScrollCaptureCallbacks.asBinder().unlinkToDeath(this, 0);
        }
        this.mActive = false;
        this.mConnected = false;
        this.mSession = null;
        this.mRemote = null;
        this.mLocal = null;
        this.mCloseGuard.close();
        Trace.endSection();
        Reference.reachabilityFence(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$close$3() {
    }

    private void cancelPendingAction() {
        synchronized (this.mLock) {
            if (this.mCancellation != null) {
                Trace.instantForTrack(2L, TRACE_TRACK, "CancellationSignal.cancel");
                Log.w(TAG, "cancelling pending operation.");
                this.mCancellation.cancel();
                this.mCancellation = null;
            }
        }
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    public boolean isActive() {
        return this.mActive;
    }

    private void checkActive() throws RemoteException {
        synchronized (this.mLock) {
            if (!this.mActive) {
                throw new RemoteException(new IllegalStateException("Not started!"));
            }
        }
    }

    public String toString() {
        return "ScrollCaptureConnection{active=" + this.mActive + ", session=" + this.mSession + ", remote=" + this.mRemote + ", local=" + this.mLocal + "}";
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    /* loaded from: classes3.dex */
    private static class SafeCallback<T> {
        private final Executor mExecutor;
        private final CancellationSignal mSignal;
        private final AtomicReference<T> mValue;

        protected SafeCallback(CancellationSignal signal, Executor executor, T value) {
            this.mSignal = signal;
            this.mValue = new AtomicReference<>(value);
            this.mExecutor = executor;
        }

        protected final void maybeAccept(final Consumer<T> consumer) {
            final T value = this.mValue.getAndSet(null);
            if (!this.mSignal.isCanceled() && value != null) {
                this.mExecutor.execute(new Runnable() { // from class: android.view.ScrollCaptureConnection$SafeCallback$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        consumer.accept(value);
                    }
                });
            }
        }

        static Runnable create(CancellationSignal signal, Executor executor, Runnable target) {
            return new RunnableCallback(signal, executor, target);
        }

        static <T> Consumer<T> create(CancellationSignal signal, Executor executor, Consumer<T> target) {
            return new ConsumerCallback(signal, executor, target);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class RunnableCallback extends SafeCallback<Runnable> implements Runnable {
        RunnableCallback(CancellationSignal signal, Executor executor, Runnable target) {
            super(signal, executor, target);
        }

        @Override // java.lang.Runnable
        public void run() {
            maybeAccept(new BinderCacheManager$BinderDeathTracker$$ExternalSyntheticLambda0());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static final class ConsumerCallback<T> extends SafeCallback<Consumer<T>> implements Consumer<T> {
        ConsumerCallback(CancellationSignal signal, Executor executor, Consumer<T> target) {
            super(signal, executor, target);
        }

        @Override // java.util.function.Consumer
        public void accept(final T value) {
            maybeAccept(new Consumer() { // from class: android.view.ScrollCaptureConnection$ConsumerCallback$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((Consumer) obj).accept(value);
                }
            });
        }
    }
}
