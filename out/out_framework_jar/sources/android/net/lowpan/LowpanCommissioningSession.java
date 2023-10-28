package android.net.lowpan;

import android.net.IpPrefix;
import android.net.lowpan.ILowpanInterfaceListener;
import android.net.lowpan.LowpanCommissioningSession;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public class LowpanCommissioningSession {
    private final LowpanBeaconInfo mBeaconInfo;
    private final ILowpanInterface mBinder;
    private Callback mCallback;
    private Handler mHandler;
    private final ILowpanInterfaceListener mInternalCallback;
    private volatile boolean mIsClosed;
    private final Looper mLooper;

    /* loaded from: classes2.dex */
    public static abstract class Callback {
        public void onReceiveFromCommissioner(byte[] packet) {
        }

        public void onClosed() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class InternalCallback extends ILowpanInterfaceListener.Stub {
        private InternalCallback() {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onStateChanged(String value) {
            if (!LowpanCommissioningSession.this.mIsClosed) {
                char c = 65535;
                switch (value.hashCode()) {
                    case -1548612125:
                        if (value.equals("offline")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 97204770:
                        if (value.equals("fault")) {
                            c = 1;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                        synchronized (LowpanCommissioningSession.this) {
                            LowpanCommissioningSession.this.lockedCleanup();
                        }
                        return;
                    default:
                        return;
                }
            }
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onReceiveFromCommissioner(final byte[] packet) {
            LowpanCommissioningSession.this.mHandler.post(new Runnable() { // from class: android.net.lowpan.LowpanCommissioningSession$InternalCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanCommissioningSession.InternalCallback.this.m2793xe7d9e4b7(packet);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceiveFromCommissioner$0$android-net-lowpan-LowpanCommissioningSession$InternalCallback  reason: not valid java name */
        public /* synthetic */ void m2793xe7d9e4b7(byte[] packet) {
            synchronized (LowpanCommissioningSession.this) {
                if (!LowpanCommissioningSession.this.mIsClosed && LowpanCommissioningSession.this.mCallback != null) {
                    LowpanCommissioningSession.this.mCallback.onReceiveFromCommissioner(packet);
                }
            }
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onEnabledChanged(boolean value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onConnectedChanged(boolean value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onUpChanged(boolean value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onRoleChanged(String value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLowpanIdentityChanged(LowpanIdentity value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkNetworkAdded(IpPrefix value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkNetworkRemoved(IpPrefix value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkAddressAdded(String value) {
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkAddressRemoved(String value) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LowpanCommissioningSession(ILowpanInterface binder, LowpanBeaconInfo beaconInfo, Looper looper) {
        InternalCallback internalCallback = new InternalCallback();
        this.mInternalCallback = internalCallback;
        this.mCallback = null;
        this.mIsClosed = false;
        this.mBinder = binder;
        this.mBeaconInfo = beaconInfo;
        this.mLooper = looper;
        if (looper != null) {
            this.mHandler = new Handler(looper);
        } else {
            this.mHandler = new Handler();
        }
        try {
            binder.addListener(internalCallback);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void lockedCleanup() {
        if (!this.mIsClosed) {
            try {
                this.mBinder.removeListener(this.mInternalCallback);
            } catch (DeadObjectException e) {
            } catch (RemoteException x) {
                throw x.rethrowAsRuntimeException();
            }
            if (this.mCallback != null) {
                this.mHandler.post(new Runnable() { // from class: android.net.lowpan.LowpanCommissioningSession$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LowpanCommissioningSession.this.m2792x5c85fe83();
                    }
                });
            }
        }
        this.mCallback = null;
        this.mIsClosed = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$lockedCleanup$0$android-net-lowpan-LowpanCommissioningSession  reason: not valid java name */
    public /* synthetic */ void m2792x5c85fe83() {
        this.mCallback.onClosed();
    }

    public LowpanBeaconInfo getBeaconInfo() {
        return this.mBeaconInfo;
    }

    public void sendToCommissioner(byte[] packet) {
        if (!this.mIsClosed) {
            try {
                this.mBinder.sendToCommissioner(packet);
            } catch (DeadObjectException e) {
            } catch (RemoteException x) {
                throw x.rethrowAsRuntimeException();
            }
        }
    }

    public synchronized void setCallback(Callback cb, Handler handler) {
        if (!this.mIsClosed) {
            if (handler != null) {
                this.mHandler = handler;
            } else if (this.mLooper != null) {
                this.mHandler = new Handler(this.mLooper);
            } else {
                this.mHandler = new Handler();
            }
            this.mCallback = cb;
        }
    }

    public synchronized void close() {
        if (!this.mIsClosed) {
            try {
                this.mBinder.closeCommissioningSession();
                lockedCleanup();
            } catch (DeadObjectException e) {
            } catch (RemoteException x) {
                throw x.rethrowAsRuntimeException();
            }
        }
    }
}
