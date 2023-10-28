package android.hardware.location;

import android.hardware.location.IActivityRecognitionHardwareSink;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IActivityRecognitionHardware extends IInterface {
    boolean disableActivityEvent(String str, int i) throws RemoteException;

    boolean enableActivityEvent(String str, int i, long j) throws RemoteException;

    boolean flush() throws RemoteException;

    String[] getSupportedActivities() throws RemoteException;

    boolean isActivitySupported(String str) throws RemoteException;

    boolean registerSink(IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink) throws RemoteException;

    boolean unregisterSink(IActivityRecognitionHardwareSink iActivityRecognitionHardwareSink) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IActivityRecognitionHardware {
        @Override // android.hardware.location.IActivityRecognitionHardware
        public String[] getSupportedActivities() throws RemoteException {
            return null;
        }

        @Override // android.hardware.location.IActivityRecognitionHardware
        public boolean isActivitySupported(String activityType) throws RemoteException {
            return false;
        }

        @Override // android.hardware.location.IActivityRecognitionHardware
        public boolean registerSink(IActivityRecognitionHardwareSink sink) throws RemoteException {
            return false;
        }

        @Override // android.hardware.location.IActivityRecognitionHardware
        public boolean unregisterSink(IActivityRecognitionHardwareSink sink) throws RemoteException {
            return false;
        }

        @Override // android.hardware.location.IActivityRecognitionHardware
        public boolean enableActivityEvent(String activityType, int eventType, long reportLatencyNs) throws RemoteException {
            return false;
        }

        @Override // android.hardware.location.IActivityRecognitionHardware
        public boolean disableActivityEvent(String activityType, int eventType) throws RemoteException {
            return false;
        }

        @Override // android.hardware.location.IActivityRecognitionHardware
        public boolean flush() throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IActivityRecognitionHardware {
        public static final String DESCRIPTOR = "android.hardware.location.IActivityRecognitionHardware";
        static final int TRANSACTION_disableActivityEvent = 6;
        static final int TRANSACTION_enableActivityEvent = 5;
        static final int TRANSACTION_flush = 7;
        static final int TRANSACTION_getSupportedActivities = 1;
        static final int TRANSACTION_isActivitySupported = 2;
        static final int TRANSACTION_registerSink = 3;
        static final int TRANSACTION_unregisterSink = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IActivityRecognitionHardware asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IActivityRecognitionHardware)) {
                return (IActivityRecognitionHardware) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public static String getDefaultTransactionName(int transactionCode) {
            switch (transactionCode) {
                case 1:
                    return "getSupportedActivities";
                case 2:
                    return "isActivitySupported";
                case 3:
                    return "registerSink";
                case 4:
                    return "unregisterSink";
                case 5:
                    return "enableActivityEvent";
                case 6:
                    return "disableActivityEvent";
                case 7:
                    return "flush";
                default:
                    return null;
            }
        }

        @Override // android.os.Binder
        public String getTransactionName(int transactionCode) {
            return getDefaultTransactionName(transactionCode);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String[] _result = getSupportedActivities();
                            reply.writeNoException();
                            reply.writeStringArray(_result);
                            break;
                        case 2:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result2 = isActivitySupported(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 3:
                            IActivityRecognitionHardwareSink _arg02 = IActivityRecognitionHardwareSink.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result3 = registerSink(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 4:
                            IActivityRecognitionHardwareSink _arg03 = IActivityRecognitionHardwareSink.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            boolean _result4 = unregisterSink(_arg03);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 5:
                            String _arg04 = data.readString();
                            int _arg1 = data.readInt();
                            long _arg2 = data.readLong();
                            data.enforceNoDataAvail();
                            boolean _result5 = enableActivityEvent(_arg04, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 6:
                            String _arg05 = data.readString();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result6 = disableActivityEvent(_arg05, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 7:
                            boolean _result7 = flush();
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IActivityRecognitionHardware {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public String[] getSupportedActivities() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public boolean isActivitySupported(String activityType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(activityType);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public boolean registerSink(IActivityRecognitionHardwareSink sink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(sink);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public boolean unregisterSink(IActivityRecognitionHardwareSink sink) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(sink);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public boolean enableActivityEvent(String activityType, int eventType, long reportLatencyNs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(activityType);
                    _data.writeInt(eventType);
                    _data.writeLong(reportLatencyNs);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public boolean disableActivityEvent(String activityType, int eventType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(activityType);
                    _data.writeInt(eventType);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.location.IActivityRecognitionHardware
            public boolean flush() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 6;
        }
    }
}
