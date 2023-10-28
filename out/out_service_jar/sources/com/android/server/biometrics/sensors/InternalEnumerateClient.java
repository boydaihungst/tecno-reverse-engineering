package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.os.IBinder;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class InternalEnumerateClient<T> extends HalClientMonitor<T> implements EnumerateConsumer {
    private static final String TAG = "Biometrics/InternalEnumerateClient";
    private List<? extends BiometricAuthenticator.Identifier> mEnrolledList;
    private List<BiometricAuthenticator.Identifier> mUnknownHALTemplates;
    private BiometricUtils mUtils;

    /* JADX INFO: Access modifiers changed from: protected */
    public InternalEnumerateClient(Context context, Supplier<T> lazyDaemon, IBinder token, int userId, String owner, List<? extends BiometricAuthenticator.Identifier> enrolledList, BiometricUtils utils, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, null, userId, owner, 0, sensorId, logger, biometricContext);
        this.mUnknownHALTemplates = new ArrayList();
        this.mEnrolledList = enrolledList;
        this.mUtils = utils;
    }

    @Override // com.android.server.biometrics.sensors.EnumerateConsumer
    public void onEnumerationResult(BiometricAuthenticator.Identifier identifier, int remaining) {
        handleEnumeratedTemplate(identifier);
        if (remaining == 0) {
            doTemplateCleanup();
            this.mCallback.onClientFinished(this, true);
        }
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        startHalOperation();
    }

    private void handleEnumeratedTemplate(BiometricAuthenticator.Identifier identifier) {
        if (identifier == null) {
            Slog.d(TAG, "Null identifier");
            return;
        }
        Slog.v(TAG, "handleEnumeratedTemplate: " + identifier.getBiometricId());
        boolean matched = false;
        int i = 0;
        while (true) {
            if (i >= this.mEnrolledList.size()) {
                break;
            } else if (this.mEnrolledList.get(i).getBiometricId() != identifier.getBiometricId()) {
                i++;
            } else {
                this.mEnrolledList.remove(i);
                matched = true;
                break;
            }
        }
        if (!matched && identifier.getBiometricId() != 0) {
            this.mUnknownHALTemplates.add(identifier);
        }
        Slog.v(TAG, "Matched: " + matched);
    }

    private void doTemplateCleanup() {
        if (this.mEnrolledList == null) {
            Slog.d(TAG, "Null enrolledList");
            return;
        }
        for (int i = 0; i < this.mEnrolledList.size(); i++) {
            BiometricAuthenticator.Identifier identifier = this.mEnrolledList.get(i);
            Slog.e(TAG, "doTemplateCleanup(): Removing dangling template from framework: " + identifier.getBiometricId() + " " + ((Object) identifier.getName()));
            this.mUtils.removeBiometricForUser(getContext(), getTargetUserId(), identifier.getBiometricId());
            getLogger().logUnknownEnrollmentInFramework();
        }
        this.mEnrolledList.clear();
    }

    public List<BiometricAuthenticator.Identifier> getUnknownHALTemplates() {
        return this.mUnknownHALTemplates;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 6;
    }
}
