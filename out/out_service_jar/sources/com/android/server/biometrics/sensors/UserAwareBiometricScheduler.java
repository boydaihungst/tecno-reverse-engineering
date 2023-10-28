package com.android.server.biometrics.sensors;

import android.hardware.biometrics.IBiometricService;
import android.os.Handler;
import android.os.ServiceManager;
import android.util.Slog;
import com.android.server.biometrics.sensors.UserAwareBiometricScheduler;
import com.android.server.biometrics.sensors.fingerprint.GestureAvailabilityDispatcher;
/* loaded from: classes.dex */
public class UserAwareBiometricScheduler extends BiometricScheduler {
    private static final String BASE_TAG = "UaBiometricScheduler";
    private final CurrentUserRetriever mCurrentUserRetriever;
    private StopUserClient<?> mStopUserClient;
    private final UserSwitchCallback mUserSwitchCallback;

    /* loaded from: classes.dex */
    public interface CurrentUserRetriever {
        int getCurrentUserId();
    }

    /* loaded from: classes.dex */
    public interface UserSwitchCallback {
        StartUserClient<?, ?> getStartUserClient(int i);

        StopUserClient<?> getStopUserClient(int i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ClientFinishedCallback implements ClientMonitorCallback {
        private final BaseClientMonitor mOwner;

        ClientFinishedCallback(BaseClientMonitor owner) {
            this.mOwner = owner;
        }

        @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
        public void onClientFinished(final BaseClientMonitor clientMonitor, final boolean success) {
            UserAwareBiometricScheduler.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.UserAwareBiometricScheduler$ClientFinishedCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UserAwareBiometricScheduler.ClientFinishedCallback.this.m2320xf69889f5(clientMonitor, success);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onClientFinished$0$com-android-server-biometrics-sensors-UserAwareBiometricScheduler$ClientFinishedCallback  reason: not valid java name */
        public /* synthetic */ void m2320xf69889f5(BaseClientMonitor clientMonitor, boolean success) {
            Slog.d(UserAwareBiometricScheduler.this.getTag(), "[Client finished] " + clientMonitor + ", success: " + success);
            if (UserAwareBiometricScheduler.this.mCurrentOperation != null && UserAwareBiometricScheduler.this.mCurrentOperation.isFor(this.mOwner)) {
                UserAwareBiometricScheduler.this.mCurrentOperation = null;
            } else {
                Slog.w(UserAwareBiometricScheduler.this.getTag(), "operation is already null or different (reset?): " + UserAwareBiometricScheduler.this.mCurrentOperation);
            }
            UserAwareBiometricScheduler.this.startNextOperationIfIdle();
        }
    }

    public UserAwareBiometricScheduler(String tag, Handler handler, int sensorType, GestureAvailabilityDispatcher gestureAvailabilityDispatcher, IBiometricService biometricService, CurrentUserRetriever currentUserRetriever, UserSwitchCallback userSwitchCallback, CoexCoordinator coexCoordinator) {
        super(tag, handler, sensorType, gestureAvailabilityDispatcher, biometricService, 50, coexCoordinator);
        this.mCurrentUserRetriever = currentUserRetriever;
        this.mUserSwitchCallback = userSwitchCallback;
    }

    public UserAwareBiometricScheduler(String tag, int sensorType, GestureAvailabilityDispatcher gestureAvailabilityDispatcher, CurrentUserRetriever currentUserRetriever, UserSwitchCallback userSwitchCallback) {
        this(tag, new Handler(getBiometricLooper()), sensorType, gestureAvailabilityDispatcher, IBiometricService.Stub.asInterface(ServiceManager.getService("biometric")), currentUserRetriever, userSwitchCallback, CoexCoordinator.getInstance());
    }

    @Override // com.android.server.biometrics.sensors.BiometricScheduler
    protected String getTag() {
        return "UaBiometricScheduler/" + this.mBiometricTag;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.BiometricScheduler
    public void startNextOperationIfIdle() {
        if (this.mCurrentOperation != null) {
            Slog.v(getTag(), "Not idle, current operation: " + this.mCurrentOperation);
        } else if (this.mPendingOperations.isEmpty()) {
            Slog.d(getTag(), "No operations, returning to idle");
        } else {
            int currentUserId = this.mCurrentUserRetriever.getCurrentUserId();
            int nextUserId = this.mPendingOperations.getFirst().getTargetUserId();
            if (nextUserId == currentUserId) {
                super.startNextOperationIfIdle();
            } else if (currentUserId == -10000) {
                BaseClientMonitor startClient = this.mUserSwitchCallback.getStartUserClient(nextUserId);
                ClientFinishedCallback finishedCallback = new ClientFinishedCallback(startClient);
                Slog.d(getTag(), "[Starting User] " + startClient);
                this.mCurrentOperation = new BiometricSchedulerOperation(startClient, finishedCallback, 2);
                startClient.start(finishedCallback);
            } else if (this.mStopUserClient != null) {
                Slog.d(getTag(), "[Waiting for StopUser] " + this.mStopUserClient);
            } else {
                StopUserClient<?> stopUserClient = this.mUserSwitchCallback.getStopUserClient(currentUserId);
                this.mStopUserClient = stopUserClient;
                ClientFinishedCallback finishedCallback2 = new ClientFinishedCallback(stopUserClient);
                Slog.d(getTag(), "[Stopping User] current: " + currentUserId + ", next: " + nextUserId + ". " + this.mStopUserClient);
                this.mCurrentOperation = new BiometricSchedulerOperation(this.mStopUserClient, finishedCallback2, 2);
                this.mStopUserClient.start(finishedCallback2);
            }
        }
    }

    public void onUserStopped() {
        if (this.mStopUserClient == null) {
            Slog.e(getTag(), "Unexpected onUserStopped");
            return;
        }
        Slog.d(getTag(), "[OnUserStopped]: " + this.mStopUserClient);
        this.mStopUserClient.onUserStopped();
        this.mStopUserClient = null;
    }
}
