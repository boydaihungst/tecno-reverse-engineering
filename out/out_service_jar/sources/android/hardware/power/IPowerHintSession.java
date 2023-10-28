package android.hardware.power;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IPowerHintSession extends IInterface {
    public static final String DESCRIPTOR = "android$hardware$power$IPowerHintSession".replace('$', '.');
    public static final String HASH = "fd3434f993d791e75d959a042010dd6fca13e33c";
    public static final int VERSION = 3;

    void close() throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void pause() throws RemoteException;

    void reportActualWorkDuration(WorkDuration[] workDurationArr) throws RemoteException;

    void resume() throws RemoteException;

    void updateTargetWorkDuration(long j) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IPowerHintSession {
        @Override // android.hardware.power.IPowerHintSession
        public void updateTargetWorkDuration(long targetDurationNanos) throws RemoteException {
        }

        @Override // android.hardware.power.IPowerHintSession
        public void reportActualWorkDuration(WorkDuration[] durations) throws RemoteException {
        }

        @Override // android.hardware.power.IPowerHintSession
        public void pause() throws RemoteException {
        }

        @Override // android.hardware.power.IPowerHintSession
        public void resume() throws RemoteException {
        }

        @Override // android.hardware.power.IPowerHintSession
        public void close() throws RemoteException {
        }

        @Override // android.hardware.power.IPowerHintSession
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // android.hardware.power.IPowerHintSession
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IPowerHintSession {
        static final int TRANSACTION_close = 5;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_pause = 3;
        static final int TRANSACTION_reportActualWorkDuration = 2;
        static final int TRANSACTION_resume = 4;
        static final int TRANSACTION_updateTargetWorkDuration = 1;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IPowerHintSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPowerHintSession)) {
                return (IPowerHintSession) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case 16777215:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            long _arg0 = data.readLong();
                            data.enforceNoDataAvail();
                            updateTargetWorkDuration(_arg0);
                            break;
                        case 2:
                            WorkDuration[] _arg02 = (WorkDuration[]) data.createTypedArray(WorkDuration.CREATOR);
                            data.enforceNoDataAvail();
                            reportActualWorkDuration(_arg02);
                            break;
                        case 3:
                            pause();
                            break;
                        case 4:
                            resume();
                            break;
                        case 5:
                            close();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IPowerHintSession {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override // android.hardware.power.IPowerHintSession
            public void updateTargetWorkDuration(long targetDurationNanos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(targetDurationNanos);
                    boolean _status = this.mRemote.transact(1, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method updateTargetWorkDuration is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.IPowerHintSession
            public void reportActualWorkDuration(WorkDuration[] durations) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedArray(durations, 0);
                    boolean _status = this.mRemote.transact(2, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method reportActualWorkDuration is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.IPowerHintSession
            public void pause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(3, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method pause is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.IPowerHintSession
            public void resume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(4, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method resume is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.IPowerHintSession
            public void close() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(5, _data, null, 1);
                    if (!_status) {
                        throw new RemoteException("Method close is unimplemented.");
                    }
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.hardware.power.IPowerHintSession
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        this.mRemote.transact(16777215, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // android.hardware.power.IPowerHintSession
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
