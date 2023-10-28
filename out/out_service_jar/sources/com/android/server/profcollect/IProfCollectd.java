package com.android.server.profcollect;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.server.profcollect.IProviderStatusCallback;
/* loaded from: classes2.dex */
public interface IProfCollectd extends IInterface {
    public static final String DESCRIPTOR = "com.android.server.profcollect.IProfCollectd";

    String get_supported_provider() throws RemoteException;

    void process() throws RemoteException;

    void registerProviderStatusCallback(IProviderStatusCallback iProviderStatusCallback) throws RemoteException;

    String report() throws RemoteException;

    void schedule() throws RemoteException;

    void terminate() throws RemoteException;

    void trace_once(String str) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IProfCollectd {
        @Override // com.android.server.profcollect.IProfCollectd
        public void schedule() throws RemoteException {
        }

        @Override // com.android.server.profcollect.IProfCollectd
        public void terminate() throws RemoteException {
        }

        @Override // com.android.server.profcollect.IProfCollectd
        public void trace_once(String tag) throws RemoteException {
        }

        @Override // com.android.server.profcollect.IProfCollectd
        public void process() throws RemoteException {
        }

        @Override // com.android.server.profcollect.IProfCollectd
        public String report() throws RemoteException {
            return null;
        }

        @Override // com.android.server.profcollect.IProfCollectd
        public String get_supported_provider() throws RemoteException {
            return null;
        }

        @Override // com.android.server.profcollect.IProfCollectd
        public void registerProviderStatusCallback(IProviderStatusCallback cb) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IProfCollectd {
        static final int TRANSACTION_get_supported_provider = 6;
        static final int TRANSACTION_process = 4;
        static final int TRANSACTION_registerProviderStatusCallback = 7;
        static final int TRANSACTION_report = 5;
        static final int TRANSACTION_schedule = 1;
        static final int TRANSACTION_terminate = 2;
        static final int TRANSACTION_trace_once = 3;

        public Stub() {
            attachInterface(this, IProfCollectd.DESCRIPTOR);
        }

        public static IProfCollectd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IProfCollectd.DESCRIPTOR);
            if (iin != null && (iin instanceof IProfCollectd)) {
                return (IProfCollectd) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IProfCollectd.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IProfCollectd.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            schedule();
                            reply.writeNoException();
                            break;
                        case 2:
                            terminate();
                            reply.writeNoException();
                            break;
                        case 3:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            trace_once(_arg0);
                            reply.writeNoException();
                            break;
                        case 4:
                            process();
                            reply.writeNoException();
                            break;
                        case 5:
                            String _result = report();
                            reply.writeNoException();
                            reply.writeString(_result);
                            break;
                        case 6:
                            String _result2 = get_supported_provider();
                            reply.writeNoException();
                            reply.writeString(_result2);
                            break;
                        case 7:
                            IProviderStatusCallback _arg02 = IProviderStatusCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerProviderStatusCallback(_arg02);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IProfCollectd {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IProfCollectd.DESCRIPTOR;
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public void schedule() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public void terminate() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public void trace_once(String tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    _data.writeString(tag);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public void process() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public String report() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public String get_supported_provider() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.server.profcollect.IProfCollectd
            public void registerProviderStatusCallback(IProviderStatusCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IProfCollectd.DESCRIPTOR);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
