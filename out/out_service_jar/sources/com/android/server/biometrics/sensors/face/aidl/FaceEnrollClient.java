package com.android.server.biometrics.sensors.face.aidl;

import android.content.Context;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.common.NativeHandle;
import android.hardware.face.Face;
import android.hardware.face.FaceEnrollFrame;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Surface;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.EnrollClient;
import com.android.server.biometrics.sensors.face.FaceService;
import com.android.server.biometrics.sensors.face.FaceUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class FaceEnrollClient extends EnrollClient<AidlSession> {
    private static final String TAG = "FaceEnrollClient";
    private ICancellationSignal mCancellationSignal;
    private final boolean mDebugConsent;
    private final int[] mDisabledFeatures;
    private final int[] mEnrollIgnoreList;
    private final int[] mEnrollIgnoreListVendor;
    private NativeHandle mHwPreviewHandle;
    private final int mMaxTemplatesPerUser;
    private android.os.NativeHandle mOsPreviewHandle;
    private final ClientMonitorCallback mPreviewHandleDeleterCallback;
    private final Surface mPreviewSurface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FaceEnrollClient(Context context, Supplier<AidlSession> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, byte[] hardwareAuthToken, String opPackageName, long requestId, BiometricUtils<Face> utils, int[] disabledFeatures, int timeoutSec, Surface previewSurface, int sensorId, BiometricLogger logger, BiometricContext biometricContext, int maxTemplatesPerUser, boolean debugConsent) {
        super(context, lazyDaemon, token, listener, userId, hardwareAuthToken, opPackageName, utils, timeoutSec, sensorId, false, logger, biometricContext);
        this.mPreviewHandleDeleterCallback = new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.face.aidl.FaceEnrollClient.1
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientStarted(BaseClientMonitor clientMonitor) {
            }

            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
                FaceEnrollClient.this.releaseSurfaceHandlesIfNeeded();
            }
        };
        setRequestId(requestId);
        this.mEnrollIgnoreList = getContext().getResources().getIntArray(17236063);
        this.mEnrollIgnoreListVendor = getContext().getResources().getIntArray(17236066);
        this.mMaxTemplatesPerUser = maxTemplatesPerUser;
        this.mDebugConsent = debugConsent;
        this.mDisabledFeatures = disabledFeatures;
        this.mPreviewSurface = previewSurface;
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient, com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        super.start(callback);
        BiometricNotificationUtils.cancelReEnrollNotification(getContext());
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(this.mPreviewHandleDeleterCallback, getLogger().createALSCallback(true), callback);
    }

    @Override // com.android.server.biometrics.sensors.EnrollClient
    protected boolean hasReachedEnrollmentLimit() {
        return FaceUtils.getInstance(getSensorId()).getBiometricsForUser(getContext(), getTargetUserId()).size() >= this.mMaxTemplatesPerUser;
    }

    private boolean shouldSendAcquiredMessage(int acquireInfo, int vendorCode) {
        return acquireInfo == 22 ? !Utils.listContains(this.mEnrollIgnoreListVendor, vendorCode) : !Utils.listContains(this.mEnrollIgnoreList, acquireInfo);
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(int acquireInfo, int vendorCode) {
        boolean shouldSend = shouldSendAcquiredMessage(acquireInfo, vendorCode);
        onAcquiredInternal(acquireInfo, vendorCode, shouldSend);
    }

    public void onEnrollmentFrame(FaceEnrollFrame frame) {
        int acquireInfo = frame.getData().getAcquiredInfo();
        int vendorCode = frame.getData().getVendorCode();
        onAcquiredInternal(acquireInfo, vendorCode, false);
        boolean shouldSend = shouldSendAcquiredMessage(acquireInfo, vendorCode);
        if (shouldSend && getListener() != null) {
            try {
                getListener().onEnrollmentFrame(frame);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to send enrollment frame", e);
                this.mCallback.onClientFinished(this, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void startHalOperation() {
        int[] iArr;
        obtainSurfaceHandlesIfNeeded();
        try {
            List<Byte> featureList = new ArrayList<>();
            if (this.mDebugConsent) {
                featureList.add((byte) 2);
            }
            boolean shouldAddDiversePoses = true;
            for (int disabledFeature : this.mDisabledFeatures) {
                if (AidlConversionUtils.convertFrameworkToAidlFeature(disabledFeature) == 1) {
                    shouldAddDiversePoses = false;
                }
            }
            if (shouldAddDiversePoses) {
                featureList.add((byte) 1);
            }
            byte[] features = new byte[featureList.size()];
            for (int i = 0; i < featureList.size(); i++) {
                features[i] = featureList.get(i).byteValue();
            }
            this.mCancellationSignal = doEnroll(features);
        } catch (RemoteException | IllegalArgumentException e) {
            Slog.e(TAG, "Exception when requesting enroll", e);
            onError(2, 0);
            this.mCallback.onClientFinished(this, false);
        }
    }

    private ICancellationSignal doEnroll(byte[] features) throws RemoteException {
        AidlSession session = getFreshDaemon();
        HardwareAuthToken hat = HardwareAuthTokenUtils.toHardwareAuthToken(this.mHardwareAuthToken);
        if (session.hasContextMethods()) {
            return session.getSession().enrollWithContext(hat, (byte) 0, features, this.mHwPreviewHandle, getOperationContext());
        }
        return session.getSession().enroll(hat, (byte) 0, features, this.mHwPreviewHandle);
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    protected void stopHalOperation() {
        ICancellationSignal iCancellationSignal = this.mCancellationSignal;
        if (iCancellationSignal != null) {
            try {
                iCancellationSignal.cancel();
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception when requesting cancel", e);
                onError(1, 0);
                this.mCallback.onClientFinished(this, false);
            }
        }
    }

    private void obtainSurfaceHandlesIfNeeded() {
        Surface surface = this.mPreviewSurface;
        if (surface != null) {
            android.os.NativeHandle acquireSurfaceHandle = FaceService.acquireSurfaceHandle(surface);
            this.mOsPreviewHandle = acquireSurfaceHandle;
            try {
                this.mHwPreviewHandle = AidlNativeHandleUtils.dup(acquireSurfaceHandle);
                Slog.v(TAG, "Obtained handles for the preview surface.");
            } catch (IOException e) {
                this.mHwPreviewHandle = null;
                Slog.e(TAG, "Failed to dup mOsPreviewHandle", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseSurfaceHandlesIfNeeded() {
        if (this.mPreviewSurface != null && this.mHwPreviewHandle == null) {
            Slog.w(TAG, "mHwPreviewHandle is null even though mPreviewSurface is not null.");
        }
        if (this.mHwPreviewHandle != null) {
            try {
                Slog.v(TAG, "Closing mHwPreviewHandle");
                AidlNativeHandleUtils.close(this.mHwPreviewHandle);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to close mPreviewSurface", e);
            }
            this.mHwPreviewHandle = null;
        }
        if (this.mOsPreviewHandle != null) {
            Slog.v(TAG, "Releasing mOsPreviewHandle");
            FaceService.releaseSurfaceHandle(this.mOsPreviewHandle);
            this.mOsPreviewHandle = null;
        }
        if (this.mPreviewSurface != null) {
            Slog.v(TAG, "Releasing mPreviewSurface");
            this.mPreviewSurface.release();
        }
    }
}
