package com.android.server.biometrics.sensors;

import android.hardware.biometrics.IBiometricService;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.biometrics.BiometricService;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.fingerprint.GestureAvailabilityDispatcher;
import com.android.server.biometrics.sensors.fingerprint.hidl.FingerprintResetLockoutClient;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class BiometricScheduler {
    private static final String BASE_TAG = "BiometricScheduler";
    protected static final int LOG_NUM_RECENT_OPERATIONS = 50;
    public static final int SENSOR_TYPE_FACE = 1;
    public static final int SENSOR_TYPE_FP_OTHER = 3;
    public static final int SENSOR_TYPE_UDFPS = 2;
    public static final int SENSOR_TYPE_UNKNOWN = 0;
    private final IBiometricService mBiometricService;
    protected final String mBiometricTag;
    private final CoexCoordinator mCoexCoordinator;
    private final ArrayDeque<CrashState> mCrashStates;
    BiometricSchedulerOperation mCurrentOperation;
    private final GestureAvailabilityDispatcher mGestureAvailabilityDispatcher;
    protected final Handler mHandler;
    private final ClientMonitorCallback mInternalCallback;
    final Deque<BiometricSchedulerOperation> mPendingOperations;
    private final List<Integer> mRecentOperations;
    private final int mRecentOperationsLimit;
    private final int mSensorType;
    private int mTotalOperationsHandled;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface SensorType {
    }

    public static Looper getBiometricLooper() {
        return BiometricService.getBiometricLooper();
    }

    public static int sensorTypeFromFingerprintProperties(FingerprintSensorPropertiesInternal props) {
        if (props.isAnyUdfpsType()) {
            return 2;
        }
        return 3;
    }

    public static String sensorTypeToString(int sensorType) {
        switch (sensorType) {
            case 0:
                return "Unknown";
            case 1:
                return "Face";
            case 2:
                return "Udfps";
            case 3:
                return "OtherFp";
            default:
                return "UnknownUnknown";
        }
    }

    /* loaded from: classes.dex */
    private static final class CrashState {
        static final int NUM_ENTRIES = 10;
        final String currentOperation;
        final List<String> pendingOperations;
        final String timestamp;

        CrashState(String timestamp, String currentOperation, List<String> pendingOperations) {
            this.timestamp = timestamp;
            this.currentOperation = currentOperation;
            this.pendingOperations = pendingOperations;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.timestamp).append(": ");
            sb.append("Current Operation: {").append(this.currentOperation).append("}");
            sb.append(", Pending Operations(").append(this.pendingOperations.size()).append(")");
            if (!this.pendingOperations.isEmpty()) {
                sb.append(": ");
            }
            for (int i = 0; i < this.pendingOperations.size(); i++) {
                sb.append(this.pendingOperations.get(i));
                if (i < this.pendingOperations.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.sensors.BiometricScheduler$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements ClientMonitorCallback {
        AnonymousClass1() {
        }

        @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
        public void onClientStarted(BaseClientMonitor clientMonitor) {
            Slog.d(BiometricScheduler.this.getTag(), "[Started] " + clientMonitor);
            if (clientMonitor instanceof AuthenticationClient) {
                BiometricScheduler.this.mCoexCoordinator.addAuthenticationClient(BiometricScheduler.this.mSensorType, (AuthenticationClient) clientMonitor);
            }
        }

        @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
        public void onClientFinished(final BaseClientMonitor clientMonitor, final boolean success) {
            BiometricScheduler.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.BiometricScheduler$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricScheduler.AnonymousClass1.this.m2308xed45abb5(clientMonitor, success);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onClientFinished$0$com-android-server-biometrics-sensors-BiometricScheduler$1  reason: not valid java name */
        public /* synthetic */ void m2308xed45abb5(BaseClientMonitor clientMonitor, boolean success) {
            if (BiometricScheduler.this.mCurrentOperation == null) {
                Slog.e(BiometricScheduler.this.getTag(), "[Finishing] " + clientMonitor + " but current operation is null, success: " + success + ", possible lifecycle bug in clientMonitor implementation?");
            } else if (!BiometricScheduler.this.mCurrentOperation.isFor(clientMonitor)) {
                Slog.e(BiometricScheduler.this.getTag(), "[Ignoring Finish] " + clientMonitor + " does not match current: " + BiometricScheduler.this.mCurrentOperation);
            } else {
                Slog.d(BiometricScheduler.this.getTag(), "[Finishing] " + clientMonitor + ", success: " + success);
                if (clientMonitor instanceof AuthenticationClient) {
                    BiometricScheduler.this.mCoexCoordinator.removeAuthenticationClient(BiometricScheduler.this.mSensorType, (AuthenticationClient) clientMonitor);
                }
                ITranFingerprintService.Instance().onClientFinished(clientMonitor, success, BiometricScheduler.this.mPendingOperations);
                if (BiometricScheduler.this.mGestureAvailabilityDispatcher != null) {
                    BiometricScheduler.this.mGestureAvailabilityDispatcher.markSensorActive(BiometricScheduler.this.mCurrentOperation.getSensorId(), false);
                }
                if (BiometricScheduler.this.mRecentOperations.size() >= BiometricScheduler.this.mRecentOperationsLimit) {
                    BiometricScheduler.this.mRecentOperations.remove(0);
                }
                BiometricScheduler.this.mRecentOperations.add(Integer.valueOf(BiometricScheduler.this.mCurrentOperation.getProtoEnum()));
                BiometricScheduler.this.mCurrentOperation = null;
                BiometricScheduler.this.mTotalOperationsHandled++;
                BiometricScheduler.this.startNextOperationIfIdle();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricScheduler(String tag, Handler handler, int sensorType, GestureAvailabilityDispatcher gestureAvailabilityDispatcher, IBiometricService biometricService, int recentOperationsLimit, CoexCoordinator coexCoordinator) {
        this.mInternalCallback = new AnonymousClass1();
        this.mBiometricTag = tag;
        this.mHandler = handler;
        this.mSensorType = sensorType;
        this.mGestureAvailabilityDispatcher = gestureAvailabilityDispatcher;
        this.mPendingOperations = new ArrayDeque();
        this.mBiometricService = biometricService;
        this.mCrashStates = new ArrayDeque<>();
        this.mRecentOperationsLimit = recentOperationsLimit;
        this.mRecentOperations = new ArrayList();
        this.mCoexCoordinator = coexCoordinator;
    }

    public BiometricScheduler(String tag, int sensorType, GestureAvailabilityDispatcher gestureAvailabilityDispatcher) {
        this(tag, new Handler(getBiometricLooper()), sensorType, gestureAvailabilityDispatcher, IBiometricService.Stub.asInterface(ServiceManager.getService("biometric")), 50, CoexCoordinator.getInstance());
    }

    public ClientMonitorCallback getInternalCallback() {
        return this.mInternalCallback;
    }

    protected String getTag() {
        return "BiometricScheduler/" + this.mBiometricTag;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void startNextOperationIfIdle() {
        if (this.mCurrentOperation != null) {
            Slog.v(getTag(), "Not idle, current operation: " + this.mCurrentOperation);
        } else if (this.mPendingOperations.isEmpty()) {
            Slog.d(getTag(), "No operations, returning to idle");
        } else {
            this.mCurrentOperation = this.mPendingOperations.poll();
            Slog.d(getTag(), "[Polled] " + this.mCurrentOperation);
            ITranFingerprintService.Instance().scheduleClientMonitor(getCurrentClient());
            if (this.mCurrentOperation.isMarkedCanceling()) {
                Slog.d(getTag(), "[Now Cancelling] " + this.mCurrentOperation);
                ITranFingerprintService.Instance().cancelInternal(this.mPendingOperations);
                this.mCurrentOperation.cancel(this.mHandler, this.mInternalCallback);
                return;
            }
            if (this.mGestureAvailabilityDispatcher != null && this.mCurrentOperation.isAcquisitionOperation()) {
                this.mGestureAvailabilityDispatcher.markSensorActive(this.mCurrentOperation.getSensorId(), true);
            }
            int cookie = this.mCurrentOperation.isReadyToStart(this.mInternalCallback);
            if (cookie == 0) {
                if (!this.mCurrentOperation.start(this.mInternalCallback)) {
                    int pendingOperationsLength = this.mPendingOperations.size();
                    BiometricSchedulerOperation lastOperation = this.mPendingOperations.peekLast();
                    Slog.e(getTag(), "[Unable To Start] " + this.mCurrentOperation + ". Last pending operation: " + lastOperation);
                    for (int i = 0; i < pendingOperationsLength; i++) {
                        BiometricSchedulerOperation operation = this.mPendingOperations.pollFirst();
                        if (operation != null) {
                            Slog.w(getTag(), "[Aborting Operation] " + operation);
                            operation.abort();
                        } else {
                            Slog.e(getTag(), "Null operation, index: " + i + ", expected length: " + pendingOperationsLength);
                        }
                    }
                    this.mCurrentOperation = null;
                    startNextOperationIfIdle();
                    return;
                }
                return;
            }
            try {
                this.mBiometricService.onReadyForAuthentication(this.mCurrentOperation.getClientMonitor().getRequestId(), cookie);
            } catch (RemoteException e) {
                Slog.e(getTag(), "Remote exception when contacting BiometricService", e);
            }
            Slog.d(getTag(), "Waiting for cookie before starting: " + this.mCurrentOperation);
        }
    }

    public void startPreparedClient(int cookie) {
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        if (biometricSchedulerOperation == null) {
            Slog.e(getTag(), "Current operation is null");
        } else if (biometricSchedulerOperation.startWithCookie(this.mInternalCallback, cookie)) {
            Slog.d(getTag(), "[Started] Prepared client: " + this.mCurrentOperation);
        } else {
            Slog.e(getTag(), "[Unable To Start] Prepared client: " + this.mCurrentOperation);
            this.mCurrentOperation = null;
            startNextOperationIfIdle();
        }
    }

    public void scheduleClientMonitor(BaseClientMonitor clientMonitor) {
        scheduleClientMonitor(clientMonitor, null);
    }

    public void scheduleClientMonitor(BaseClientMonitor clientMonitor, ClientMonitorCallback clientCallback) {
        BiometricSchedulerOperation biometricSchedulerOperation;
        if (clientMonitor.interruptsPrecedingClients()) {
            for (BiometricSchedulerOperation operation : this.mPendingOperations) {
                if (operation.markCanceling()) {
                    Slog.d(getTag(), "New client, marking pending op as canceling: " + operation);
                }
            }
        }
        this.mPendingOperations.add(new BiometricSchedulerOperation(clientMonitor, clientCallback));
        Slog.d(getTag(), "[Added] " + clientMonitor + ", new queue size: " + this.mPendingOperations.size());
        if (clientMonitor.interruptsPrecedingClients() && (biometricSchedulerOperation = this.mCurrentOperation) != null && biometricSchedulerOperation.isInterruptable() && this.mCurrentOperation.isStarted() && (!(clientMonitor instanceof FingerprintResetLockoutClient) || !"com.whatsapp".equals(getCurrentClient().getOwnerString()) || getCurrentClient().isAlreadyDone())) {
            Slog.d(getTag(), "[Cancelling Interruptable]: " + this.mCurrentOperation);
            ITranFingerprintService.Instance().cancelInternal(this.mPendingOperations);
            this.mCurrentOperation.cancel(this.mHandler, this.mInternalCallback);
            return;
        }
        startNextOperationIfIdle();
    }

    public void cancelEnrollment(IBinder token, long requestId) {
        Slog.d(getTag(), "cancelEnrollment, requestId: " + requestId);
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        if (biometricSchedulerOperation != null && canCancelEnrollOperation(biometricSchedulerOperation, token, requestId)) {
            Slog.d(getTag(), "Cancelling enrollment op: " + this.mCurrentOperation);
            ITranFingerprintService.Instance().cancelInternal(this.mPendingOperations);
            this.mCurrentOperation.cancel(this.mHandler, this.mInternalCallback);
            return;
        }
        for (BiometricSchedulerOperation operation : this.mPendingOperations) {
            if (canCancelEnrollOperation(operation, token, requestId)) {
                Slog.d(getTag(), "Cancelling pending enrollment op: " + operation);
                operation.markCanceling();
            }
        }
    }

    public void cancelAuthenticationOrDetection(IBinder token, long requestId) {
        Slog.d(getTag(), "cancelAuthenticationOrDetection, requestId: " + requestId);
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        if (biometricSchedulerOperation != null && canCancelAuthOperation(biometricSchedulerOperation, token, requestId)) {
            Slog.d(getTag(), "Cancelling auth/detect op: " + this.mCurrentOperation);
            ITranFingerprintService.Instance().cancelInternal(this.mPendingOperations);
            this.mCurrentOperation.cancel(this.mHandler, this.mInternalCallback);
            return;
        }
        for (BiometricSchedulerOperation operation : this.mPendingOperations) {
            if (canCancelAuthOperation(operation, token, requestId)) {
                Slog.d(getTag(), "Cancelling pending auth/detect op: " + operation);
                operation.markCanceling();
            }
        }
    }

    private static boolean canCancelEnrollOperation(BiometricSchedulerOperation operation, IBinder token, long requestId) {
        return operation.isEnrollOperation() && operation.isMatchingToken(token) && operation.isMatchingRequestId(requestId);
    }

    private static boolean canCancelAuthOperation(BiometricSchedulerOperation operation, IBinder token, long requestId) {
        return operation.isAuthenticationOrDetectionOperation() && operation.isMatchingToken(token) && operation.isMatchingRequestId(requestId);
    }

    @Deprecated
    public BaseClientMonitor getCurrentClient() {
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        if (biometricSchedulerOperation != null) {
            return biometricSchedulerOperation.getClientMonitor();
        }
        return null;
    }

    @Deprecated
    public void getCurrentClientIfMatches(final long requestId, final Consumer<BaseClientMonitor> clientMonitorConsumer) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.BiometricScheduler$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BiometricScheduler.this.m2307x4db4cad6(requestId, clientMonitorConsumer);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCurrentClientIfMatches$0$com-android-server-biometrics-sensors-BiometricScheduler  reason: not valid java name */
    public /* synthetic */ void m2307x4db4cad6(long requestId, Consumer clientMonitorConsumer) {
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        if (biometricSchedulerOperation != null && biometricSchedulerOperation.isMatchingRequestId(requestId)) {
            clientMonitorConsumer.accept(this.mCurrentOperation.getClientMonitor());
        } else {
            clientMonitorConsumer.accept(null);
        }
    }

    public int getCurrentPendingCount() {
        return this.mPendingOperations.size();
    }

    public void recordCrashState() {
        if (this.mCrashStates.size() >= 10) {
            this.mCrashStates.removeFirst();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        String timestamp = dateFormat.format(new Date(System.currentTimeMillis()));
        List<String> pendingOperations = new ArrayList<>();
        for (BiometricSchedulerOperation operation : this.mPendingOperations) {
            pendingOperations.add(operation.toString());
        }
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        CrashState crashState = new CrashState(timestamp, biometricSchedulerOperation != null ? biometricSchedulerOperation.toString() : null, pendingOperations);
        this.mCrashStates.add(crashState);
        Slog.e(getTag(), "Recorded crash state: " + crashState.toString());
    }

    public void dump(PrintWriter pw) {
        pw.println("Dump of BiometricScheduler " + getTag());
        pw.println("Type: " + this.mSensorType);
        pw.println("Current operation: " + this.mCurrentOperation);
        pw.println("Pending operations: " + this.mPendingOperations.size());
        for (BiometricSchedulerOperation operation : this.mPendingOperations) {
            pw.println("Pending operation: " + operation);
        }
        Iterator<CrashState> it = this.mCrashStates.iterator();
        while (it.hasNext()) {
            CrashState crashState = it.next();
            pw.println("Crash State " + crashState);
        }
    }

    public byte[] dumpProtoState(boolean clearSchedulerBuffer) {
        ProtoOutputStream proto = new ProtoOutputStream();
        BiometricSchedulerOperation biometricSchedulerOperation = this.mCurrentOperation;
        proto.write(1159641169921L, biometricSchedulerOperation != null ? biometricSchedulerOperation.getProtoEnum() : 0);
        proto.write(1120986464258L, this.mTotalOperationsHandled);
        if (!this.mRecentOperations.isEmpty()) {
            for (int i = 0; i < this.mRecentOperations.size(); i++) {
                proto.write(2259152797699L, this.mRecentOperations.get(i).intValue());
            }
        } else {
            proto.write(2259152797699L, 0);
        }
        proto.flush();
        if (clearSchedulerBuffer) {
            this.mRecentOperations.clear();
        }
        return proto.getBytes();
    }

    public void reset() {
        Slog.d(getTag(), "Resetting scheduler");
        this.mPendingOperations.clear();
        this.mCurrentOperation = null;
        ITranFingerprintService.Instance().reset();
    }
}
