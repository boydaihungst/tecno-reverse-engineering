package com.android.server.biometrics.sensors.fingerprint;

import android.content.ComponentName;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.IBinder;
import android.util.proto.ProtoOutputStream;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes.dex */
public interface ServiceProvider {
    void cancelAuthentication(int i, IBinder iBinder, long j);

    void cancelEnrollment(int i, IBinder iBinder, long j);

    boolean checkName(int i, int i2, String str);

    boolean containsSensor(int i);

    ITestSession createTestSession(int i, ITestSessionCallback iTestSessionCallback, String str);

    void dumpInternal(int i, PrintWriter printWriter);

    void dumpProtoMetrics(int i, FileDescriptor fileDescriptor);

    void dumpProtoState(int i, ProtoOutputStream protoOutputStream, boolean z);

    Fingerprint getAddBiometricForUser(int i, int i2);

    String getAppPackagenameForUser(int i, int i2, int i3);

    int getAppUserIdForUser(int i, int i2, int i3);

    long getAuthenticatorId(int i, int i2);

    List<Fingerprint> getEnrolledFingerprints(int i, int i2);

    int getLockoutModeForUser(int i, int i2);

    FingerprintSensorPropertiesInternal getSensorProperties(int i);

    List<FingerprintSensorPropertiesInternal> getSensorProperties();

    boolean hasAppPackagename(int i, int i2);

    boolean isHardwareDetected(int i);

    void notifyAppPauseForFp(int i, int i2, String str, String str2);

    void notifyAppResumeForFp(int i, int i2, String str, String str2);

    void onPointerDown(long j, int i, int i2, int i3, float f, float f2);

    void onPointerUp(long j, int i);

    void onUiReady(long j, int i);

    void onUpdateFocusedApp(int i, String str, ComponentName componentName, String str2, ComponentName componentName2);

    void rename(int i, int i2, int i3, String str);

    long scheduleAuthenticate(int i, IBinder iBinder, long j, int i2, int i3, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str, boolean z, int i4, boolean z2);

    void scheduleAuthenticate(int i, IBinder iBinder, long j, int i2, int i3, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str, long j2, boolean z, int i4, boolean z2);

    long scheduleEnroll(int i, IBinder iBinder, byte[] bArr, int i2, IFingerprintServiceReceiver iFingerprintServiceReceiver, String str, int i3);

    long scheduleFingerDetect(int i, IBinder iBinder, int i2, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str, int i3);

    void scheduleGenerateChallenge(int i, int i2, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, String str);

    void scheduleInternalCleanup(int i, int i2, ClientMonitorCallback clientMonitorCallback);

    void scheduleInvalidateAuthenticatorId(int i, int i2, IInvalidationCallback iInvalidationCallback);

    void scheduleRemove(int i, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i2, int i3, String str);

    void scheduleRemoveAll(int i, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i2, String str);

    void scheduleResetLockout(int i, int i2, byte[] bArr);

    void scheduleRevokeChallenge(int i, int i2, IBinder iBinder, String str, long j);

    void setAppBiometricsForUser(int i, int i2, int i3, String str, int i4);

    void setSidefpsController(ISidefpsController iSidefpsController);

    void setUdfpsOverlayController(IUdfpsOverlayController iUdfpsOverlayController);

    void startAppForFp(int i, int i2, int i3);

    void startPreparedClient(int i, int i2);
}
