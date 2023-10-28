package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricAuthenticator.Identifier;
import android.os.IBinder;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class InternalCleanupClient<S extends BiometricAuthenticator.Identifier, T> extends HalClientMonitor<T> implements EnumerateConsumer, RemovalConsumer, EnrollmentModifier {
    private static final String TAG = "Biometrics/InternalCleanupClient";
    private final Map<Integer, Long> mAuthenticatorIds;
    private final BiometricUtils<S> mBiometricUtils;
    private BaseClientMonitor mCurrentTask;
    private final List<S> mEnrolledList;
    private final ClientMonitorCallback mEnumerateCallback;
    private final boolean mHasEnrollmentsBeforeStarting;
    private final ClientMonitorCallback mRemoveCallback;
    private final ArrayList<UserTemplate> mUnknownHALTemplates;

    protected abstract InternalEnumerateClient<T> getEnumerateClient(Context context, Supplier<T> supplier, IBinder iBinder, int i, String str, List<S> list, BiometricUtils<S> biometricUtils, int i2, BiometricLogger biometricLogger, BiometricContext biometricContext);

    protected abstract RemovalClient<S, T> getRemovalClient(Context context, Supplier<T> supplier, IBinder iBinder, int i, int i2, String str, BiometricUtils<S> biometricUtils, int i3, BiometricLogger biometricLogger, BiometricContext biometricContext, Map<Integer, Long> map);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class UserTemplate {
        final BiometricAuthenticator.Identifier mIdentifier;
        final int mUserId;

        UserTemplate(BiometricAuthenticator.Identifier identifier, int userId) {
            this.mIdentifier = identifier;
            this.mUserId = userId;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public InternalCleanupClient(Context context, Supplier<T> lazyDaemon, int userId, String owner, int sensorId, BiometricLogger logger, BiometricContext biometricContext, List<S> enrolledList, BiometricUtils<S> utils, Map<Integer, Long> authenticatorIds) {
        super(context, lazyDaemon, null, null, userId, owner, 0, sensorId, logger, biometricContext);
        this.mUnknownHALTemplates = new ArrayList<>();
        this.mEnumerateCallback = new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.InternalCleanupClient.1
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                List<BiometricAuthenticator.Identifier> unknownHALTemplates = ((InternalEnumerateClient) InternalCleanupClient.this.mCurrentTask).getUnknownHALTemplates();
                Slog.d(InternalCleanupClient.TAG, "Enumerate onClientFinished: " + clientMonitor + ", success: " + success);
                if (!unknownHALTemplates.isEmpty()) {
                    Slog.w(InternalCleanupClient.TAG, "Adding " + unknownHALTemplates.size() + " templates for deletion");
                }
                for (BiometricAuthenticator.Identifier unknownHALTemplate : unknownHALTemplates) {
                    InternalCleanupClient.this.mUnknownHALTemplates.add(new UserTemplate(unknownHALTemplate, InternalCleanupClient.this.mCurrentTask.getTargetUserId()));
                }
                if (InternalCleanupClient.this.mUnknownHALTemplates.isEmpty()) {
                    InternalCleanupClient.this.mCallback.onClientFinished(InternalCleanupClient.this, success);
                } else {
                    InternalCleanupClient.this.startCleanupUnknownHalTemplates();
                }
            }
        };
        this.mRemoveCallback = new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.InternalCleanupClient.2
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                Slog.d(InternalCleanupClient.TAG, "Remove onClientFinished: " + clientMonitor + ", success: " + success);
                InternalCleanupClient.this.mCallback.onClientFinished(InternalCleanupClient.this, success);
            }
        };
        this.mBiometricUtils = utils;
        this.mAuthenticatorIds = authenticatorIds;
        this.mEnrolledList = enrolledList;
        this.mHasEnrollmentsBeforeStarting = !utils.getBiometricsForUser(context, userId).isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startCleanupUnknownHalTemplates() {
        Slog.d(TAG, "startCleanupUnknownHalTemplates, size: " + this.mUnknownHALTemplates.size());
        UserTemplate template = this.mUnknownHALTemplates.get(0);
        this.mUnknownHALTemplates.remove(template);
        this.mCurrentTask = getRemovalClient(getContext(), this.mLazyDaemon, getToken(), template.mIdentifier.getBiometricId(), template.mUserId, getContext().getPackageName(), this.mBiometricUtils, getSensorId(), getLogger(), getBiometricContext(), this.mAuthenticatorIds);
        getLogger().logUnknownEnrollmentInHal();
        this.mCurrentTask.start(this.mRemoveCallback);
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        this.mCurrentTask = getEnumerateClient(getContext(), this.mLazyDaemon, getToken(), getTargetUserId(), getOwnerString(), this.mEnrolledList, this.mBiometricUtils, getSensorId(), getLogger(), getBiometricContext());
        Slog.d(TAG, "Starting enumerate: " + this.mCurrentTask);
        this.mCurrentTask.start(this.mEnumerateCallback);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
    }

    @Override // com.android.server.biometrics.sensors.RemovalConsumer
    public void onRemoved(BiometricAuthenticator.Identifier identifier, int remaining) {
        BaseClientMonitor baseClientMonitor = this.mCurrentTask;
        if (!(baseClientMonitor instanceof RemovalClient)) {
            Slog.e(TAG, "onRemoved received during client: " + this.mCurrentTask.getClass().getSimpleName());
        } else {
            ((RemovalClient) baseClientMonitor).onRemoved(identifier, remaining);
        }
    }

    @Override // com.android.server.biometrics.sensors.EnrollmentModifier
    public boolean hasEnrollmentStateChanged() {
        boolean hasEnrollmentsNow = !this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).isEmpty();
        return hasEnrollmentsNow != this.mHasEnrollmentsBeforeStarting;
    }

    @Override // com.android.server.biometrics.sensors.EnrollmentModifier
    public boolean hasEnrollments() {
        return !this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).isEmpty();
    }

    @Override // com.android.server.biometrics.sensors.EnumerateConsumer
    public void onEnumerationResult(BiometricAuthenticator.Identifier identifier, int remaining) {
        if (!(this.mCurrentTask instanceof InternalEnumerateClient)) {
            Slog.e(TAG, "onEnumerationResult received during client: " + this.mCurrentTask.getClass().getSimpleName());
            return;
        }
        Slog.d(TAG, "onEnumerated, remaining: " + remaining);
        ((EnumerateConsumer) this.mCurrentTask).onEnumerationResult(identifier, remaining);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 7;
    }
}
