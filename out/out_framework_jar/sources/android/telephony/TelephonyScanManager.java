package android.telephony;

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.telephony.TelephonyScanManager;
import android.util.SparseArray;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
/* loaded from: classes3.dex */
public final class TelephonyScanManager {
    public static final int CALLBACK_RESTRICTED_SCAN_RESULTS = 4;
    public static final int CALLBACK_SCAN_COMPLETE = 3;
    public static final int CALLBACK_SCAN_ERROR = 2;
    public static final int CALLBACK_SCAN_RESULTS = 1;
    public static final int CALLBACK_TELEPHONY_DIED = 5;
    public static final int INVALID_SCAN_ID = -1;
    public static final String SCAN_RESULT_KEY = "scanResult";
    private static final String TAG = "TelephonyScanManager";
    private final IBinder.DeathRecipient mDeathRecipient;
    private final Handler mHandler;
    private final Looper mLooper;
    private final Messenger mMessenger;
    private final SparseArray<NetworkScanInfo> mScanInfo = new SparseArray<>();

    /* loaded from: classes3.dex */
    public static abstract class NetworkScanCallback {
        public void onResults(List<CellInfo> results) {
        }

        public void onComplete() {
        }

        public void onError(int error) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class NetworkScanInfo {
        private final NetworkScanCallback mCallback;
        private final Executor mExecutor;
        private final NetworkScanRequest mRequest;

        NetworkScanInfo(NetworkScanRequest request, Executor executor, NetworkScanCallback callback) {
            this.mRequest = request;
            this.mExecutor = executor;
            this.mCallback = callback;
        }
    }

    public TelephonyScanManager() {
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        Looper looper = thread.getLooper();
        this.mLooper = looper;
        AnonymousClass1 anonymousClass1 = new AnonymousClass1(looper);
        this.mHandler = anonymousClass1;
        this.mMessenger = new Messenger(anonymousClass1);
        this.mDeathRecipient = new IBinder.DeathRecipient() { // from class: android.telephony.TelephonyScanManager.2
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                TelephonyScanManager.this.mHandler.obtainMessage(5).sendToTarget();
            }
        };
    }

    /* renamed from: android.telephony.TelephonyScanManager$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    class AnonymousClass1 extends Handler {
        AnonymousClass1(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            NetworkScanInfo nsi;
            Preconditions.checkNotNull(message, "message cannot be null");
            if (message.what == 5) {
                synchronized (TelephonyScanManager.this.mScanInfo) {
                    for (int i = 0; i < TelephonyScanManager.this.mScanInfo.size(); i++) {
                        NetworkScanInfo nsi2 = (NetworkScanInfo) TelephonyScanManager.this.mScanInfo.valueAt(i);
                        if (nsi2 != null) {
                            Executor e = nsi2.mExecutor;
                            final NetworkScanCallback cb = nsi2.mCallback;
                            if (e != null && cb != null) {
                                try {
                                    e.execute(new Runnable() { // from class: android.telephony.TelephonyScanManager$1$$ExternalSyntheticLambda0
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            TelephonyScanManager.NetworkScanCallback.this.onError(3);
                                        }
                                    });
                                } catch (RejectedExecutionException e2) {
                                }
                            }
                        }
                    }
                    TelephonyScanManager.this.mScanInfo.clear();
                }
                return;
            }
            synchronized (TelephonyScanManager.this.mScanInfo) {
                nsi = (NetworkScanInfo) TelephonyScanManager.this.mScanInfo.get(message.arg2);
            }
            if (nsi == null) {
                throw new RuntimeException("Failed to find NetworkScanInfo with id " + message.arg2);
            }
            final NetworkScanCallback callback = nsi.mCallback;
            Executor executor = nsi.mExecutor;
            switch (message.what) {
                case 1:
                case 4:
                    try {
                        Bundle b = message.getData();
                        Parcelable[] parcelables = b.getParcelableArray(TelephonyScanManager.SCAN_RESULT_KEY);
                        final CellInfo[] ci = new CellInfo[parcelables.length];
                        for (int i2 = 0; i2 < parcelables.length; i2++) {
                            ci[i2] = (CellInfo) parcelables[i2];
                        }
                        executor.execute(new Runnable() { // from class: android.telephony.TelephonyScanManager$1$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                TelephonyScanManager.AnonymousClass1.lambda$handleMessage$1(ci, callback);
                            }
                        });
                        return;
                    } catch (Exception e3) {
                        com.android.telephony.Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onResults", e3);
                        return;
                    }
                case 2:
                    try {
                        final int errorCode = message.arg1;
                        executor.execute(new Runnable() { // from class: android.telephony.TelephonyScanManager$1$$ExternalSyntheticLambda2
                            @Override // java.lang.Runnable
                            public final void run() {
                                TelephonyScanManager.AnonymousClass1.lambda$handleMessage$2(errorCode, callback);
                            }
                        });
                        synchronized (TelephonyScanManager.this.mScanInfo) {
                            TelephonyScanManager.this.mScanInfo.remove(message.arg2);
                        }
                        return;
                    } catch (Exception e4) {
                        com.android.telephony.Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onError", e4);
                        return;
                    }
                case 3:
                    try {
                        executor.execute(new Runnable() { // from class: android.telephony.TelephonyScanManager$1$$ExternalSyntheticLambda3
                            @Override // java.lang.Runnable
                            public final void run() {
                                TelephonyScanManager.AnonymousClass1.lambda$handleMessage$3(TelephonyScanManager.NetworkScanCallback.this);
                            }
                        });
                        synchronized (TelephonyScanManager.this.mScanInfo) {
                            TelephonyScanManager.this.mScanInfo.remove(message.arg2);
                        }
                        return;
                    } catch (Exception e5) {
                        com.android.telephony.Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onComplete", e5);
                        return;
                    }
                default:
                    com.android.telephony.Rlog.e(TelephonyScanManager.TAG, "Unhandled message " + Integer.toHexString(message.what));
                    return;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$handleMessage$1(CellInfo[] ci, NetworkScanCallback callback) {
            com.android.telephony.Rlog.d(TelephonyScanManager.TAG, "onResults: " + ci.toString());
            callback.onResults(Arrays.asList(ci));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$handleMessage$2(int errorCode, NetworkScanCallback callback) {
            com.android.telephony.Rlog.d(TelephonyScanManager.TAG, "onError: " + errorCode);
            callback.onError(errorCode);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$handleMessage$3(NetworkScanCallback callback) {
            com.android.telephony.Rlog.d(TelephonyScanManager.TAG, "onComplete");
            callback.onComplete();
        }
    }

    public NetworkScan requestNetworkScan(int subId, boolean renounceFineLocationAccess, NetworkScanRequest request, Executor executor, NetworkScanCallback callback, String callingPackage, String callingFeatureId) {
        try {
            Objects.requireNonNull(request, "Request was null");
            Objects.requireNonNull(callback, "Callback was null");
            Objects.requireNonNull(executor, "Executor was null");
            ITelephony telephony = getITelephony();
            if (telephony == null) {
                return null;
            }
            try {
                synchronized (this.mScanInfo) {
                    try {
                        int scanId = telephony.requestNetworkScan(subId, renounceFineLocationAccess, request, this.mMessenger, new Binder(), callingPackage, callingFeatureId);
                        if (scanId == -1) {
                            com.android.telephony.Rlog.e(TAG, "Failed to initiate network scan");
                            return null;
                        }
                        telephony.asBinder().linkToDeath(this.mDeathRecipient, 0);
                        saveScanInfo(scanId, request, executor, callback);
                        return new NetworkScan(scanId, subId);
                    } catch (Throwable th) {
                        th = th;
                        try {
                            throw th;
                        } catch (RemoteException e) {
                            ex = e;
                            com.android.telephony.Rlog.e(TAG, "requestNetworkScan RemoteException", ex);
                            return null;
                        } catch (NullPointerException e2) {
                            ex = e2;
                            com.android.telephony.Rlog.e(TAG, "requestNetworkScan NPE", ex);
                            return null;
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (RemoteException e3) {
            ex = e3;
        } catch (NullPointerException e4) {
            ex = e4;
        }
    }

    private void saveScanInfo(int id, NetworkScanRequest request, Executor executor, NetworkScanCallback callback) {
        this.mScanInfo.put(id, new NetworkScanInfo(request, executor, callback));
    }

    private ITelephony getITelephony() {
        return ITelephony.Stub.asInterface(TelephonyFrameworkInitializer.getTelephonyServiceManager().getTelephonyServiceRegisterer().get());
    }
}
