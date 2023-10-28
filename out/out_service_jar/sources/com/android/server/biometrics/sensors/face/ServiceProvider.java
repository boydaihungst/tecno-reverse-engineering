package com.android.server.biometrics.sensors.face;

import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.face.Face;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceServiceReceiver;
import android.os.IBinder;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
/* loaded from: classes.dex */
public interface ServiceProvider {
    void cancelAuthentication(int i, IBinder iBinder, long j);

    void cancelEnrollment(int i, IBinder iBinder, long j);

    void cancelFaceDetect(int i, IBinder iBinder, long j);

    boolean containsSensor(int i);

    ITestSession createTestSession(int i, ITestSessionCallback iTestSessionCallback, String str);

    void dumpHal(int i, FileDescriptor fileDescriptor, String[] strArr);

    void dumpInternal(int i, PrintWriter printWriter);

    void dumpProtoMetrics(int i, FileDescriptor fileDescriptor);

    void dumpProtoState(int i, ProtoOutputStream protoOutputStream, boolean z);

    long getAuthenticatorId(int i, int i2);

    List<Face> getEnrolledFaces(int i, int i2);

    int getLockoutModeForUser(int i, int i2);

    FaceSensorPropertiesInternal getSensorProperties(int i);

    List<FaceSensorPropertiesInternal> getSensorProperties();

    boolean isHardwareDetected(int i);

    long scheduleAuthenticate(int i, IBinder iBinder, long j, int i2, int i3, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str, boolean z, int i4, boolean z2, boolean z3);

    void scheduleAuthenticate(int i, IBinder iBinder, long j, int i2, int i3, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str, long j2, boolean z, int i4, boolean z2, boolean z3);

    long scheduleEnroll(int i, IBinder iBinder, byte[] bArr, int i2, IFaceServiceReceiver iFaceServiceReceiver, String str, int[] iArr, Surface surface, boolean z);

    long scheduleFaceDetect(int i, IBinder iBinder, int i2, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str, int i3);

    void scheduleGenerateChallenge(int i, int i2, IBinder iBinder, IFaceServiceReceiver iFaceServiceReceiver, String str);

    void scheduleGetFeature(int i, IBinder iBinder, int i2, int i3, ClientMonitorCallbackConverter clientMonitorCallbackConverter, String str);

    void scheduleInternalCleanup(int i, int i2, ClientMonitorCallback clientMonitorCallback);

    void scheduleRemove(int i, IBinder iBinder, int i2, int i3, IFaceServiceReceiver iFaceServiceReceiver, String str);

    void scheduleRemoveAll(int i, IBinder iBinder, int i2, IFaceServiceReceiver iFaceServiceReceiver, String str);

    void scheduleResetLockout(int i, int i2, byte[] bArr);

    void scheduleRevokeChallenge(int i, int i2, IBinder iBinder, String str, long j);

    void scheduleSetFeature(int i, IBinder iBinder, int i2, int i3, boolean z, byte[] bArr, IFaceServiceReceiver iFaceServiceReceiver, String str);

    void startPreparedClient(int i, int i2);

    default void scheduleInvalidateAuthenticatorId(int sensorId, int userId, IInvalidationCallback callback) {
        throw new IllegalStateException("Providers that support invalidation must override this method");
    }
}
