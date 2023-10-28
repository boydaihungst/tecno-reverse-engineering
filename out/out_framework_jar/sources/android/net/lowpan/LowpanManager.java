package android.net.lowpan;

import android.content.Context;
import android.net.lowpan.ILowpanManager;
import android.net.lowpan.ILowpanManagerListener;
import android.net.lowpan.LowpanManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.BackgroundThread;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
/* loaded from: classes2.dex */
public class LowpanManager {
    private static final String TAG = LowpanManager.class.getSimpleName();
    private final Map<IBinder, WeakReference<LowpanInterface>> mBinderCache;
    private final Context mContext;
    private final Map<String, LowpanInterface> mInterfaceCache;
    private final Map<Integer, ILowpanManagerListener> mListenerMap;
    private final Looper mLooper;
    private final ILowpanManager mService;

    /* loaded from: classes2.dex */
    public static abstract class Callback {
        public void onInterfaceAdded(LowpanInterface lowpanInterface) {
        }

        public void onInterfaceRemoved(LowpanInterface lowpanInterface) {
        }
    }

    public static LowpanManager from(Context context) {
        return (LowpanManager) context.getSystemService("lowpan");
    }

    public static LowpanManager getManager() {
        IBinder binder = ServiceManager.getService("lowpan");
        if (binder != null) {
            ILowpanManager service = ILowpanManager.Stub.asInterface(binder);
            return new LowpanManager(service);
        }
        return null;
    }

    LowpanManager(ILowpanManager service) {
        this.mListenerMap = new HashMap();
        this.mInterfaceCache = new HashMap();
        this.mBinderCache = new WeakHashMap();
        this.mService = service;
        this.mContext = null;
        this.mLooper = null;
    }

    public LowpanManager(Context context, ILowpanManager service) {
        this(context, service, BackgroundThread.get().getLooper());
    }

    public LowpanManager(Context context, ILowpanManager service, Looper looper) {
        this.mListenerMap = new HashMap();
        this.mInterfaceCache = new HashMap();
        this.mBinderCache = new WeakHashMap();
        this.mContext = context;
        this.mService = service;
        this.mLooper = looper;
    }

    public LowpanInterface getInterfaceNoCreate(ILowpanInterface ifaceService) {
        LowpanInterface iface = null;
        synchronized (this.mBinderCache) {
            if (this.mBinderCache.containsKey(ifaceService.asBinder())) {
                iface = this.mBinderCache.get(ifaceService.asBinder()).get();
            }
        }
        return iface;
    }

    public LowpanInterface getInterface(final ILowpanInterface ifaceService) {
        LowpanInterface iface = null;
        try {
            synchronized (this.mBinderCache) {
                if (this.mBinderCache.containsKey(ifaceService.asBinder())) {
                    iface = this.mBinderCache.get(ifaceService.asBinder()).get();
                }
                if (iface == null) {
                    final String ifaceName = ifaceService.getName();
                    iface = new LowpanInterface(this.mContext, ifaceService, this.mLooper);
                    synchronized (this.mInterfaceCache) {
                        this.mInterfaceCache.put(iface.getName(), iface);
                    }
                    this.mBinderCache.put(ifaceService.asBinder(), new WeakReference<>(iface));
                    ifaceService.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: android.net.lowpan.LowpanManager.1
                        @Override // android.os.IBinder.DeathRecipient
                        public void binderDied() {
                            synchronized (LowpanManager.this.mInterfaceCache) {
                                LowpanInterface iface2 = (LowpanInterface) LowpanManager.this.mInterfaceCache.get(ifaceName);
                                if (iface2 != null && iface2.getService() == ifaceService) {
                                    LowpanManager.this.mInterfaceCache.remove(ifaceName);
                                }
                            }
                        }
                    }, 0);
                }
            }
            return iface;
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public LowpanInterface getInterface(String name) {
        LowpanInterface iface = null;
        try {
            synchronized (this.mInterfaceCache) {
                if (this.mInterfaceCache.containsKey(name)) {
                    iface = this.mInterfaceCache.get(name);
                } else {
                    ILowpanInterface ifaceService = this.mService.getInterface(name);
                    if (ifaceService != null) {
                        iface = getInterface(ifaceService);
                    }
                }
            }
            return iface;
        } catch (RemoteException x) {
            throw x.rethrowFromSystemServer();
        }
    }

    public LowpanInterface getInterface() {
        String[] ifaceList = getInterfaceList();
        if (ifaceList.length > 0) {
            return getInterface(ifaceList[0]);
        }
        return null;
    }

    public String[] getInterfaceList() {
        try {
            return this.mService.getInterfaceList();
        } catch (RemoteException x) {
            throw x.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.net.lowpan.LowpanManager$2  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass2 extends ILowpanManagerListener.Stub {
        private Handler mHandler;
        final /* synthetic */ Callback val$cb;
        final /* synthetic */ Handler val$handler;

        AnonymousClass2(Handler handler, Callback callback) {
            this.val$handler = handler;
            this.val$cb = callback;
            if (handler != null) {
                this.mHandler = handler;
            } else if (LowpanManager.this.mLooper != null) {
                this.mHandler = new Handler(LowpanManager.this.mLooper);
            } else {
                this.mHandler = new Handler();
            }
        }

        @Override // android.net.lowpan.ILowpanManagerListener
        public void onInterfaceAdded(final ILowpanInterface ifaceService) {
            final Callback callback = this.val$cb;
            Runnable runnable = new Runnable() { // from class: android.net.lowpan.LowpanManager$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanManager.AnonymousClass2.this.m2810lambda$onInterfaceAdded$0$androidnetlowpanLowpanManager$2(ifaceService, callback);
                }
            };
            this.mHandler.post(runnable);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceAdded$0$android-net-lowpan-LowpanManager$2  reason: not valid java name */
        public /* synthetic */ void m2810lambda$onInterfaceAdded$0$androidnetlowpanLowpanManager$2(ILowpanInterface ifaceService, Callback cb) {
            LowpanInterface iface = LowpanManager.this.getInterface(ifaceService);
            if (iface != null) {
                cb.onInterfaceAdded(iface);
            }
        }

        @Override // android.net.lowpan.ILowpanManagerListener
        public void onInterfaceRemoved(final ILowpanInterface ifaceService) {
            final Callback callback = this.val$cb;
            Runnable runnable = new Runnable() { // from class: android.net.lowpan.LowpanManager$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    LowpanManager.AnonymousClass2.this.m2811lambda$onInterfaceRemoved$1$androidnetlowpanLowpanManager$2(ifaceService, callback);
                }
            };
            this.mHandler.post(runnable);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceRemoved$1$android-net-lowpan-LowpanManager$2  reason: not valid java name */
        public /* synthetic */ void m2811lambda$onInterfaceRemoved$1$androidnetlowpanLowpanManager$2(ILowpanInterface ifaceService, Callback cb) {
            LowpanInterface iface = LowpanManager.this.getInterfaceNoCreate(ifaceService);
            if (iface != null) {
                cb.onInterfaceRemoved(iface);
            }
        }
    }

    public void registerCallback(Callback cb, Handler handler) throws LowpanException {
        ILowpanManagerListener.Stub listenerBinder = new AnonymousClass2(handler, cb);
        try {
            this.mService.addListener(listenerBinder);
            synchronized (this.mListenerMap) {
                this.mListenerMap.put(Integer.valueOf(System.identityHashCode(cb)), listenerBinder);
            }
        } catch (RemoteException x) {
            throw x.rethrowFromSystemServer();
        }
    }

    public void registerCallback(Callback cb) throws LowpanException {
        registerCallback(cb, null);
    }

    public void unregisterCallback(Callback cb) {
        ILowpanManagerListener listenerBinder;
        Integer hashCode = Integer.valueOf(System.identityHashCode(cb));
        synchronized (this.mListenerMap) {
            listenerBinder = this.mListenerMap.get(hashCode);
            this.mListenerMap.remove(hashCode);
        }
        if (listenerBinder != null) {
            try {
                this.mService.removeListener(listenerBinder);
                return;
            } catch (RemoteException x) {
                throw x.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Attempt to unregister an unknown callback");
    }
}
