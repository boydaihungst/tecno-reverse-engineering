package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.face.Face;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Surface;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.EnrollClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceEnrollClient extends EnrollClient<IBiometricsFace> {
    private static final String TAG = "FaceEnrollClient";
    private final int[] mDisabledFeatures;
    private final int[] mEnrollIgnoreList;
    private final int[] mEnrollIgnoreListVendor;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceEnrollClient(Context context, Supplier<IBiometricsFace> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, byte[] hardwareAuthToken, String owner, long requestId, BiometricUtils<Face> utils, int[] disabledFeatures, int timeoutSec, Surface previewSurface, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, listener, userId, hardwareAuthToken, owner, utils, timeoutSec, sensorId, false, logger, biometricContext);
        setRequestId(requestId);
        this.mDisabledFeatures = Arrays.copyOf(disabledFeatures, disabledFeatures.length);
        this.mEnrollIgnoreList = getContext().getResources().getIntArray(17236063);
        this.mEnrollIgnoreListVendor = getContext().getResources().getIntArray(17236066);
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(getLogger().createALSCallback(true), callback);
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient
    protected boolean hasReachedEnrollmentLimit() {
        int limit = getContext().getResources().getInteger(17694831);
        int enrolled = this.mBiometricUtils.getBiometricsForUser(getContext(), getTargetUserId()).size();
        if (enrolled >= limit) {
            Slog.w(TAG, "Too many faces registered, user: " + getTargetUserId());
            return true;
        }
        return false;
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(int acquireInfo, int vendorCode) {
        boolean shouldSend;
        if (acquireInfo == 22) {
            shouldSend = !Utils.listContains(this.mEnrollIgnoreListVendor, vendorCode);
        } else {
            shouldSend = !Utils.listContains(this.mEnrollIgnoreList, acquireInfo);
        }
        onAcquiredInternal(acquireInfo, vendorCode, shouldSend);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        byte[] bArr;
        int[] iArr;
        ArrayList<Byte> token = new ArrayList<>();
        for (byte b : this.mHardwareAuthToken) {
            token.add(Byte.valueOf(b));
        }
        ArrayList<Integer> disabledFeatures = new ArrayList<>();
        for (int disabledFeature : this.mDisabledFeatures) {
            disabledFeatures.add(Integer.valueOf(disabledFeature));
        }
        try {
            int status = getFreshDaemon().enroll(token, this.mTimeoutSec, disabledFeatures);
            if (status != 0) {
                onError(2, 0);
                this.mCallback.onClientFinished(this, false);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting enroll", e);
            onError(2, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        try {
            getFreshDaemon().cancel();
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when requesting cancel", e);
            onError(1, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }
}
