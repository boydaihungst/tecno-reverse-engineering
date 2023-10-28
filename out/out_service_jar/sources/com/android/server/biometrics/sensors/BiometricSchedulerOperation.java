package com.android.server.biometrics.sensors;

import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.BackupAgentTimeoutParameters;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
/* loaded from: classes.dex */
public class BiometricSchedulerOperation {
    private static final int CANCEL_WATCHDOG_DELAY_MS = 3000;
    protected static final int STATE_FINISHED = 5;
    protected static final int STATE_STARTED = 2;
    protected static final int STATE_STARTED_CANCELING = 3;
    protected static final int STATE_WAITING_FOR_COOKIE = 4;
    protected static final int STATE_WAITING_IN_QUEUE = 0;
    protected static final int STATE_WAITING_IN_QUEUE_CANCELING = 1;
    protected static final String TAG = "BiometricSchedulerOperation";
    final Runnable mCancelWatchdog;
    private final ClientMonitorCallback mClientCallback;
    private final BaseClientMonitor mClientMonitor;
    private final BooleanSupplier mIsDebuggable;
    private ClientMonitorCallback mOnStartCallback;
    private int mState;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    protected @interface OperationState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricSchedulerOperation(BaseClientMonitor clientMonitor, ClientMonitorCallback callback) {
        this(clientMonitor, callback, 0);
    }

    BiometricSchedulerOperation(BaseClientMonitor clientMonitor, ClientMonitorCallback callback, BooleanSupplier isDebuggable) {
        this(clientMonitor, callback, 0, isDebuggable);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public BiometricSchedulerOperation(BaseClientMonitor clientMonitor, ClientMonitorCallback callback, int state) {
        this(clientMonitor, callback, state, new BooleanSupplier() { // from class: com.android.server.biometrics.sensors.BiometricSchedulerOperation$$ExternalSyntheticLambda0
            @Override // java.util.function.BooleanSupplier
            public final boolean getAsBoolean() {
                return Build.isDebuggable();
            }
        });
    }

    private BiometricSchedulerOperation(BaseClientMonitor clientMonitor, ClientMonitorCallback callback, int state, BooleanSupplier isDebuggable) {
        this.mClientMonitor = clientMonitor;
        this.mClientCallback = callback;
        this.mState = state;
        this.mIsDebuggable = isDebuggable;
        this.mCancelWatchdog = new Runnable() { // from class: com.android.server.biometrics.sensors.BiometricSchedulerOperation$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BiometricSchedulerOperation.this.m2311xa3c2eb09();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-biometrics-sensors-BiometricSchedulerOperation  reason: not valid java name */
    public /* synthetic */ void m2311xa3c2eb09() {
        if (!isFinished()) {
            Slog.e(TAG, "[Watchdog Triggered]: " + this);
            getWrappedCallback(this.mOnStartCallback).onClientFinished(this.mClientMonitor, false);
        }
    }

    public int isReadyToStart(ClientMonitorCallback callback) {
        int i = this.mState;
        if (i == 4 || i == 0) {
            int cookie = this.mClientMonitor.getCookie();
            if (cookie != 0) {
                this.mState = 4;
                this.mClientMonitor.waitForCookie(getWrappedCallback(callback));
            }
            return cookie;
        }
        return 0;
    }

    public boolean start(ClientMonitorCallback callback) {
        if (errorWhenNoneOf("start", 0, 4, 1)) {
            return false;
        }
        if (this.mClientMonitor.getCookie() != 0) {
            if (!this.mIsDebuggable.getAsBoolean()) {
                Slog.e(TAG, "operation requires cookie");
            } else {
                throw new IllegalStateException("operation requires cookie");
            }
        }
        return doStart(callback);
    }

    public boolean startWithCookie(ClientMonitorCallback callback, int cookie) {
        if (this.mClientMonitor.getCookie() != cookie) {
            Slog.e(TAG, "Mismatched cookie for operation: " + this + ", received: " + cookie);
            return false;
        } else if (errorWhenNoneOf("start", 0, 4, 1)) {
            return false;
        } else {
            return doStart(callback);
        }
    }

    private boolean doStart(ClientMonitorCallback callback) {
        this.mOnStartCallback = callback;
        ClientMonitorCallback cb = getWrappedCallback(callback);
        if (this.mState == 1) {
            Slog.d(TAG, "Operation marked for cancellation, cancelling now: " + this);
            cb.onClientFinished(this.mClientMonitor, true);
            BaseClientMonitor baseClientMonitor = this.mClientMonitor;
            if (baseClientMonitor instanceof ErrorConsumer) {
                ErrorConsumer errorConsumer = (ErrorConsumer) baseClientMonitor;
                errorConsumer.onError(5, 0);
            } else {
                Slog.w(TAG, "monitor cancelled but does not implement ErrorConsumer");
            }
            return false;
        } else if (isUnstartableHalOperation()) {
            Slog.v(TAG, "unable to start: " + this);
            ((HalClientMonitor) this.mClientMonitor).unableToStart();
            cb.onClientFinished(this.mClientMonitor, false);
            return false;
        } else {
            this.mState = 2;
            this.mClientMonitor.start(cb);
            Slog.v(TAG, "started: " + this);
            return true;
        }
    }

    public void abort() {
        if (errorWhenNoneOf("abort", 0, 4, 1)) {
            return;
        }
        if (isHalOperation()) {
            ((HalClientMonitor) this.mClientMonitor).unableToStart();
        }
        getWrappedCallback().onClientFinished(this.mClientMonitor, false);
        Slog.v(TAG, "Aborted: " + this);
    }

    public boolean markCanceling() {
        if (this.mState == 0 && isInterruptable()) {
            this.mState = 1;
            return true;
        }
        return false;
    }

    public void cancel(Handler handler, ClientMonitorCallback callback) {
        if (errorWhenOneOf("cancel", 5)) {
            return;
        }
        int currentState = this.mState;
        if (!isInterruptable()) {
            Slog.w(TAG, "Cannot cancel - operation not interruptable: " + this);
        } else if (currentState == 3) {
            Slog.w(TAG, "Cannot cancel - already invoked for operation: " + this);
        } else {
            this.mState = 3;
            if (currentState == 0 || currentState == 1 || currentState == 4) {
                Slog.d(TAG, "[Cancelling] Current client (without start): " + this.mClientMonitor);
                ((Interruptable) this.mClientMonitor).cancelWithoutStarting(getWrappedCallback(callback));
            } else {
                Slog.d(TAG, "[Cancelling] Current client: " + this.mClientMonitor);
                ((Interruptable) this.mClientMonitor).cancel();
            }
            handler.postDelayed(this.mCancelWatchdog, BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
        }
    }

    private ClientMonitorCallback getWrappedCallback() {
        return getWrappedCallback(null);
    }

    private ClientMonitorCallback getWrappedCallback(ClientMonitorCallback callback) {
        ClientMonitorCallback destroyCallback = new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.BiometricSchedulerOperation.1
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                Slog.d(BiometricSchedulerOperation.TAG, "[Finished / destroy]: " + clientMonitor);
                BiometricSchedulerOperation.this.mClientMonitor.destroy();
                BiometricSchedulerOperation.this.mState = 5;
            }
        };
        return new ClientMonitorCompositeCallback(destroyCallback, callback, this.mClientCallback);
    }

    public int getSensorId() {
        return this.mClientMonitor.getSensorId();
    }

    public int getProtoEnum() {
        return this.mClientMonitor.getProtoEnum();
    }

    public int getTargetUserId() {
        return this.mClientMonitor.getTargetUserId();
    }

    public boolean isFor(BaseClientMonitor clientMonitor) {
        return this.mClientMonitor == clientMonitor;
    }

    public boolean isInterruptable() {
        return this.mClientMonitor instanceof Interruptable;
    }

    private boolean isHalOperation() {
        return this.mClientMonitor instanceof HalClientMonitor;
    }

    private boolean isUnstartableHalOperation() {
        if (isHalOperation()) {
            HalClientMonitor<?> client = (HalClientMonitor) this.mClientMonitor;
            if (client.getFreshDaemon() == null) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isEnrollOperation() {
        return this.mClientMonitor instanceof EnrollClient;
    }

    public boolean isAuthenticateOperation() {
        return this.mClientMonitor instanceof AuthenticationClient;
    }

    public boolean isAuthenticationOrDetectionOperation() {
        BaseClientMonitor baseClientMonitor = this.mClientMonitor;
        boolean isAuthentication = baseClientMonitor instanceof AuthenticationConsumer;
        boolean isDetection = baseClientMonitor instanceof DetectionConsumer;
        return isAuthentication || isDetection;
    }

    public boolean isAcquisitionOperation() {
        return this.mClientMonitor instanceof AcquisitionClient;
    }

    public boolean isMatchingRequestId(long requestId) {
        return !this.mClientMonitor.hasRequestId() || this.mClientMonitor.getRequestId() == requestId;
    }

    public boolean isMatchingToken(IBinder token) {
        return this.mClientMonitor.getToken() == token;
    }

    public boolean isStarted() {
        return this.mState == 2;
    }

    public boolean isCanceling() {
        return this.mState == 3;
    }

    public boolean isFinished() {
        return this.mState == 5;
    }

    public boolean isMarkedCanceling() {
        return this.mState == 1;
    }

    @Deprecated
    public BaseClientMonitor getClientMonitor() {
        return this.mClientMonitor;
    }

    private boolean errorWhenOneOf(String op, int... states) {
        boolean isError = ArrayUtils.contains(states, this.mState);
        if (isError) {
            String err = op + ": mState must not be " + this.mState;
            if (this.mIsDebuggable.getAsBoolean()) {
                throw new IllegalStateException(err);
            }
            Slog.e(TAG, err);
        }
        return isError;
    }

    private boolean errorWhenNoneOf(String op, int... states) {
        boolean isError = !ArrayUtils.contains(states, this.mState);
        if (isError) {
            String err = op + ": mState=" + this.mState + " must be one of " + Arrays.toString(states);
            if (this.mIsDebuggable.getAsBoolean()) {
                throw new IllegalStateException(err);
            }
            Slog.e(TAG, err);
        }
        return isError;
    }

    public String toString() {
        return this.mClientMonitor + ", State: " + this.mState;
    }
}
