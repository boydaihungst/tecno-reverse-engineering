package com.android.server.backup.transport;

import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.backup.ITransportStatusCallback;
/* loaded from: classes.dex */
public class TransportStatusCallback extends ITransportStatusCallback.Stub {
    private static final int OPERATION_STATUS_DEFAULT = 0;
    private static final String TAG = "TransportStatusCallback";
    private static final int TIMEOUT_MILLIS = 300000;
    private boolean mHasCompletedOperation;
    private int mOperationStatus;
    private final int mOperationTimeout;

    public TransportStatusCallback() {
        this.mOperationStatus = 0;
        this.mHasCompletedOperation = false;
        this.mOperationTimeout = 300000;
    }

    TransportStatusCallback(int operationTimeout) {
        this.mOperationStatus = 0;
        this.mHasCompletedOperation = false;
        this.mOperationTimeout = operationTimeout;
    }

    public synchronized void onOperationCompleteWithStatus(int status) throws RemoteException {
        this.mHasCompletedOperation = true;
        this.mOperationStatus = status;
        notifyAll();
    }

    public synchronized void onOperationComplete() throws RemoteException {
        onOperationCompleteWithStatus(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getOperationStatus() {
        if (this.mHasCompletedOperation) {
            return this.mOperationStatus;
        }
        long timeoutLeft = this.mOperationTimeout;
        while (!this.mHasCompletedOperation && timeoutLeft > 0) {
            try {
                long waitStartTime = System.currentTimeMillis();
                wait(timeoutLeft);
                if (this.mHasCompletedOperation) {
                    return this.mOperationStatus;
                }
                timeoutLeft -= System.currentTimeMillis() - waitStartTime;
            } catch (InterruptedException e) {
                Slog.w(TAG, "Couldn't get operation status from transport: ", e);
            }
        }
        Slog.w(TAG, "Couldn't get operation status from transport");
        return -1000;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void reset() {
        this.mHasCompletedOperation = false;
        this.mOperationStatus = 0;
    }
}
