package android.net.lowpan;

import android.content.Context;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.lowpan.ILowpanInterfaceListener;
import android.net.lowpan.LowpanInterface;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import java.util.HashMap;
/* loaded from: classes2.dex */
public class LowpanInterface {
    public static final String EMPTY_PARTITION_ID = "";
    public static final String NETWORK_TYPE_THREAD_V1 = "org.threadgroup.thread.v1";
    public static final String ROLE_COORDINATOR = "coordinator";
    public static final String ROLE_DETACHED = "detached";
    public static final String ROLE_END_DEVICE = "end-device";
    public static final String ROLE_LEADER = "leader";
    public static final String ROLE_ROUTER = "router";
    public static final String ROLE_SLEEPY_END_DEVICE = "sleepy-end-device";
    public static final String ROLE_SLEEPY_ROUTER = "sleepy-router";
    public static final String STATE_ATTACHED = "attached";
    public static final String STATE_ATTACHING = "attaching";
    public static final String STATE_COMMISSIONING = "commissioning";
    public static final String STATE_FAULT = "fault";
    public static final String STATE_OFFLINE = "offline";
    private static final String TAG = LowpanInterface.class.getSimpleName();
    private final ILowpanInterface mBinder;
    private final HashMap<Integer, ILowpanInterfaceListener> mListenerMap = new HashMap<>();
    private final Looper mLooper;

    /* loaded from: classes2.dex */
    public static abstract class Callback {
        public void onConnectedChanged(boolean value) {
        }

        public void onEnabledChanged(boolean value) {
        }

        public void onUpChanged(boolean value) {
        }

        public void onRoleChanged(String value) {
        }

        public void onStateChanged(String state) {
        }

        public void onLowpanIdentityChanged(LowpanIdentity value) {
        }

        public void onLinkNetworkAdded(IpPrefix prefix) {
        }

        public void onLinkNetworkRemoved(IpPrefix prefix) {
        }

        public void onLinkAddressAdded(LinkAddress address) {
        }

        public void onLinkAddressRemoved(LinkAddress address) {
        }
    }

    public LowpanInterface(Context context, ILowpanInterface service, Looper looper) {
        this.mBinder = service;
        this.mLooper = looper;
    }

    public ILowpanInterface getService() {
        return this.mBinder;
    }

    public void form(LowpanProvision provision) throws LowpanException {
        try {
            this.mBinder.form(provision);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void join(LowpanProvision provision) throws LowpanException {
        try {
            this.mBinder.join(provision);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void attach(LowpanProvision provision) throws LowpanException {
        try {
            this.mBinder.attach(provision);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void leave() throws LowpanException {
        try {
            this.mBinder.leave();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public LowpanCommissioningSession startCommissioningSession(LowpanBeaconInfo beaconInfo) throws LowpanException {
        try {
            this.mBinder.startCommissioningSession(beaconInfo);
            return new LowpanCommissioningSession(this.mBinder, beaconInfo, this.mLooper);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void reset() throws LowpanException {
        try {
            this.mBinder.reset();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public String getName() {
        try {
            return this.mBinder.getName();
        } catch (DeadObjectException e) {
            return "";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public boolean isEnabled() {
        try {
            return this.mBinder.isEnabled();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public void setEnabled(boolean enabled) throws LowpanException {
        try {
            this.mBinder.setEnabled(enabled);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public boolean isUp() {
        try {
            return this.mBinder.isUp();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public boolean isConnected() {
        try {
            return this.mBinder.isConnected();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public boolean isCommissioned() {
        try {
            return this.mBinder.isCommissioned();
        } catch (DeadObjectException e) {
            return false;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String getState() {
        try {
            return this.mBinder.getState();
        } catch (DeadObjectException e) {
            return "fault";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String getPartitionId() {
        try {
            return this.mBinder.getPartitionId();
        } catch (DeadObjectException e) {
            return "";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public LowpanIdentity getLowpanIdentity() {
        try {
            return this.mBinder.getLowpanIdentity();
        } catch (DeadObjectException e) {
            return new LowpanIdentity();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String getRole() {
        try {
            return this.mBinder.getRole();
        } catch (DeadObjectException e) {
            return "detached";
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public LowpanCredential getLowpanCredential() {
        try {
            return this.mBinder.getLowpanCredential();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public String[] getSupportedNetworkTypes() throws LowpanException {
        try {
            return this.mBinder.getSupportedNetworkTypes();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public LowpanChannelInfo[] getSupportedChannels() throws LowpanException {
        try {
            return this.mBinder.getSupportedChannels();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.net.lowpan.LowpanInterface$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 extends ILowpanInterfaceListener.Stub {
        private Handler mHandler;
        final /* synthetic */ Callback val$cb;
        final /* synthetic */ Handler val$handler;

        AnonymousClass1(Handler handler, Callback callback) {
            this.val$handler = handler;
            this.val$cb = callback;
            if (handler != null) {
                this.mHandler = handler;
            } else if (LowpanInterface.this.mLooper != null) {
                this.mHandler = new Handler(LowpanInterface.this.mLooper);
            } else {
                this.mHandler = new Handler();
            }
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onEnabledChanged(final boolean value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onEnabledChanged(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onConnectedChanged(final boolean value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onConnectedChanged(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onUpChanged(final boolean value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onUpChanged(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onRoleChanged(final String value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onRoleChanged(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onStateChanged(final String value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onStateChanged(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLowpanIdentityChanged(final LowpanIdentity value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onLowpanIdentityChanged(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkNetworkAdded(final IpPrefix value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onLinkNetworkAdded(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkNetworkRemoved(final IpPrefix value) {
            Handler handler = this.mHandler;
            final Callback callback = this.val$cb;
            handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanInterface.Callback.this.onLinkNetworkRemoved(value);
                }
            });
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkAddressAdded(String value) {
            try {
                final LinkAddress la = new LinkAddress(value);
                Handler handler = this.mHandler;
                final Callback callback = this.val$cb;
                handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        LowpanInterface.Callback.this.onLinkAddressAdded(la);
                    }
                });
            } catch (IllegalArgumentException x) {
                Log.e(LowpanInterface.TAG, "onLinkAddressAdded: Bad LinkAddress \"" + value + "\", " + x);
            }
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onLinkAddressRemoved(String value) {
            try {
                final LinkAddress la = new LinkAddress(value);
                Handler handler = this.mHandler;
                final Callback callback = this.val$cb;
                handler.post(new Runnable() { // from class: android.net.lowpan.LowpanInterface$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        LowpanInterface.Callback.this.onLinkAddressRemoved(la);
                    }
                });
            } catch (IllegalArgumentException x) {
                Log.e(LowpanInterface.TAG, "onLinkAddressRemoved: Bad LinkAddress \"" + value + "\", " + x);
            }
        }

        @Override // android.net.lowpan.ILowpanInterfaceListener
        public void onReceiveFromCommissioner(byte[] packet) {
        }
    }

    public void registerCallback(Callback cb, Handler handler) {
        ILowpanInterfaceListener.Stub listenerBinder = new AnonymousClass1(handler, cb);
        try {
            this.mBinder.addListener(listenerBinder);
            synchronized (this.mListenerMap) {
                this.mListenerMap.put(Integer.valueOf(System.identityHashCode(cb)), listenerBinder);
            }
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public void registerCallback(Callback cb) {
        registerCallback(cb, null);
    }

    public void unregisterCallback(Callback cb) {
        int hashCode = System.identityHashCode(cb);
        synchronized (this.mListenerMap) {
            ILowpanInterfaceListener listenerBinder = this.mListenerMap.get(Integer.valueOf(hashCode));
            if (listenerBinder != null) {
                this.mListenerMap.remove(Integer.valueOf(hashCode));
                try {
                    this.mBinder.removeListener(listenerBinder);
                } catch (DeadObjectException e) {
                } catch (RemoteException x) {
                    throw x.rethrowAsRuntimeException();
                }
            }
        }
    }

    public LowpanScanner createScanner() {
        return new LowpanScanner(this.mBinder);
    }

    public LinkAddress[] getLinkAddresses() throws LowpanException {
        try {
            String[] linkAddressStrings = this.mBinder.getLinkAddresses();
            LinkAddress[] ret = new LinkAddress[linkAddressStrings.length];
            int i = 0;
            int length = linkAddressStrings.length;
            int i2 = 0;
            while (i2 < length) {
                String str = linkAddressStrings[i2];
                int i3 = i + 1;
                ret[i] = new LinkAddress(str);
                i2++;
                i = i3;
            }
            return ret;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public IpPrefix[] getLinkNetworks() throws LowpanException {
        try {
            return this.mBinder.getLinkNetworks();
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void addOnMeshPrefix(IpPrefix prefix, int flags) throws LowpanException {
        try {
            this.mBinder.addOnMeshPrefix(prefix, flags);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void removeOnMeshPrefix(IpPrefix prefix) {
        try {
            this.mBinder.removeOnMeshPrefix(prefix);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            Log.e(TAG, x2.toString());
        }
    }

    public void addExternalRoute(IpPrefix prefix, int flags) throws LowpanException {
        try {
            this.mBinder.addExternalRoute(prefix, flags);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            throw LowpanException.rethrowFromServiceSpecificException(x2);
        }
    }

    public void removeExternalRoute(IpPrefix prefix) {
        try {
            this.mBinder.removeExternalRoute(prefix);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        } catch (ServiceSpecificException x2) {
            Log.e(TAG, x2.toString());
        }
    }
}
