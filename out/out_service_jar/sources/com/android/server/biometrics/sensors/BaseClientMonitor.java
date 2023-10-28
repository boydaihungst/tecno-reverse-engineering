package com.android.server.biometrics.sensors;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.NoSuchElementException;
/* loaded from: classes.dex */
public abstract class BaseClientMonitor implements IBinder.DeathRecipient {
    protected static final boolean DEBUG = true;
    private static final String TAG = "BaseClientMonitor";
    private static int sCount = 0;
    private final BiometricContext mBiometricContext;
    private final Context mContext;
    private final int mCookie;
    private ClientMonitorCallbackConverter mListener;
    private final BiometricLogger mLogger;
    private final String mOwner;
    private long mRequestId;
    private final int mSensorId;
    private final int mSequentialId;
    private final int mTargetUserId;
    private IBinder mToken;
    private boolean mAlreadyDone = false;
    protected ClientMonitorCallback mCallback = new ClientMonitorCallback() { // from class: com.android.server.biometrics.sensors.BaseClientMonitor.1
        @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
        public void onClientStarted(BaseClientMonitor clientMonitor) {
            Slog.e(BaseClientMonitor.TAG, "mCallback onClientStarted: called before set (should not happen)");
        }

        @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
        public void onClientFinished(BaseClientMonitor clientMonitor, boolean success) {
            Slog.e(BaseClientMonitor.TAG, "mCallback onClientFinished: called before set (should not happen)");
        }
    };

    public abstract int getProtoEnum();

    public BaseClientMonitor(Context context, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int cookie, int sensorId, BiometricLogger logger, BiometricContext biometricContext) {
        int i = sCount;
        sCount = i + 1;
        this.mSequentialId = i;
        this.mContext = context;
        this.mToken = token;
        this.mRequestId = -1L;
        this.mListener = listener;
        this.mTargetUserId = userId;
        this.mOwner = owner;
        this.mCookie = cookie;
        this.mSensorId = sensorId;
        this.mLogger = logger;
        this.mBiometricContext = biometricContext;
        if (token != null) {
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.w(TAG, "caught remote exception in linkToDeath: ", e);
            }
        }
    }

    public boolean interruptsPrecedingClients() {
        return false;
    }

    public void waitForCookie(ClientMonitorCallback callback) {
        this.mCallback = callback;
    }

    public void start(ClientMonitorCallback callback) {
        ClientMonitorCallback wrapCallbackForStart = wrapCallbackForStart(callback);
        this.mCallback = wrapCallbackForStart;
        wrapCallbackForStart.onClientStarted(this);
    }

    protected ClientMonitorCallback wrapCallbackForStart(ClientMonitorCallback callback) {
        return callback;
    }

    public void destroy() {
        this.mAlreadyDone = true;
        IBinder iBinder = this.mToken;
        if (iBinder != null) {
            try {
                iBinder.unlinkToDeath(this, 0);
            } catch (NoSuchElementException e) {
                Slog.e(TAG, "destroy(): " + this + ":", new Exception("here"));
            }
            this.mToken = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void markAlreadyDone() {
        Slog.d(TAG, "marking operation as done: " + this);
        this.mAlreadyDone = true;
    }

    public boolean isAlreadyDone() {
        return this.mAlreadyDone;
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        binderDiedInternal(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void binderDiedInternal(boolean clearListener) {
        Slog.e(TAG, "Binder died, operation: " + this);
        if (this.mAlreadyDone) {
            Slog.w(TAG, "Binder died but client is finished, ignoring");
            return;
        }
        if (this instanceof Interruptable) {
            Slog.e(TAG, "Binder died, cancelling client");
            ((Interruptable) this).cancel();
        }
        this.mToken = null;
        if (clearListener) {
            this.mListener = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isCryptoOperation() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public BiometricContext getBiometricContext() {
        return this.mBiometricContext;
    }

    public BiometricLogger getLogger() {
        return this.mLogger;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final String getOwnerString() {
        return this.mOwner;
    }

    public final ClientMonitorCallbackConverter getListener() {
        return this.mListener;
    }

    public int getTargetUserId() {
        return this.mTargetUserId;
    }

    public final IBinder getToken() {
        return this.mToken;
    }

    public int getSensorId() {
        return this.mSensorId;
    }

    public int getCookie() {
        return this.mCookie;
    }

    public long getRequestId() {
        return this.mRequestId;
    }

    public boolean hasRequestId() {
        return this.mRequestId > 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void setRequestId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("request id must be positive");
        }
        this.mRequestId = id;
    }

    public ClientMonitorCallback getCallback() {
        return this.mCallback;
    }

    public String toString() {
        return "{[" + this.mSequentialId + "] " + getClass().getName() + ", proto=" + getProtoEnum() + ", owner=" + getOwnerString() + ", cookie=" + getCookie() + ", requestId=" + getRequestId() + ", userId=" + getTargetUserId() + "}";
    }
}
