package com.android.server.biometrics.sensors;

import android.content.Context;
import android.hardware.biometrics.common.OperationContext;
import android.os.IBinder;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class HalClientMonitor<T> extends BaseClientMonitor {
    protected final Supplier<T> mLazyDaemon;
    private final OperationContext mOperationContext;

    /* JADX INFO: Access modifiers changed from: protected */
    public abstract void startHalOperation();

    public abstract void unableToStart();

    public HalClientMonitor(Context context, Supplier<T> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int cookie, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext) {
        super(context, token, listener, userId, owner, cookie, sensorId, biometricLogger, biometricContext);
        this.mOperationContext = new OperationContext();
        this.mLazyDaemon = lazyDaemon;
    }

    public T getFreshDaemon() {
        return this.mLazyDaemon.get();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void destroy() {
        super.destroy();
        unsubscribeBiometricContext();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public OperationContext getOperationContext() {
        return getBiometricContext().updateContext(this.mOperationContext, isCryptoOperation());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ClientMonitorCallback getBiometricContextUnsubscriber() {
        return new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.HalClientMonitor.1
            @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
            public void onClientFinished(BaseClientMonitor monitor, boolean success) {
                HalClientMonitor.this.unsubscribeBiometricContext();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void unsubscribeBiometricContext() {
        getBiometricContext().unsubscribe(this.mOperationContext);
    }
}
