package com.mediatek.anr;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.reflect.Method;
/* loaded from: classes.dex */
public abstract class AnrManagerNative extends Binder implements IAnrManager {
    private static Method sGetService = getServiceManagerMethod("getService", new Class[]{String.class});
    private static final Singleton<IAnrManager> gDefault = new Singleton<IAnrManager>() { // from class: com.mediatek.anr.AnrManagerNative.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.mediatek.anr.AnrManagerNative.Singleton
        public IAnrManager create() {
            IBinder binder = null;
            try {
                binder = (IBinder) AnrManagerNative.sGetService.invoke(null, "anrmanager");
            } catch (Exception e) {
            }
            return AnrManagerNative.asInterface(binder);
        }
    };

    private static Method getServiceManagerMethod(String func, Class[] cls) {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            return serviceManager.getDeclaredMethod(func, cls);
        } catch (Exception e) {
            return null;
        }
    }

    public static IAnrManager asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IAnrManager in = (IAnrManager) obj.queryLocalInterface(IAnrManager.descriptor);
        if (in != null) {
            return in;
        }
        return new AnrManagerProxy(obj);
    }

    public static IAnrManager getDefault() {
        return gDefault.get();
    }

    public AnrManagerNative() {
        attachInterface(this, IAnrManager.descriptor);
    }

    @Override // android.os.Binder
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case 2:
                data.enforceInterface(IAnrManager.descriptor);
                String msgInfo = data.readString();
                int pid = data.readInt();
                informMessageDump(msgInfo, pid);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this;
    }

    /* loaded from: classes.dex */
    static abstract class Singleton<T> {
        private T mInstance;

        protected abstract T create();

        Singleton() {
        }

        public final T get() {
            T t;
            synchronized (this) {
                if (this.mInstance == null) {
                    this.mInstance = create();
                }
                t = this.mInstance;
            }
            return t;
        }
    }
}
