package android.app.timedetector;

import android.app.time.ExternalTimeSuggestion;
import android.app.time.TimeCapabilitiesAndConfig;
import android.app.time.TimeConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ITimeDetectorService extends IInterface {
    public static final String DESCRIPTOR = "android.app.timedetector.ITimeDetectorService";

    TimeCapabilitiesAndConfig getCapabilitiesAndConfig() throws RemoteException;

    void suggestExternalTime(ExternalTimeSuggestion externalTimeSuggestion) throws RemoteException;

    void suggestGnssTime(GnssTimeSuggestion gnssTimeSuggestion) throws RemoteException;

    boolean suggestManualTime(ManualTimeSuggestion manualTimeSuggestion) throws RemoteException;

    void suggestNetworkTime(NetworkTimeSuggestion networkTimeSuggestion) throws RemoteException;

    void suggestTelephonyTime(TelephonyTimeSuggestion telephonyTimeSuggestion) throws RemoteException;

    boolean updateConfiguration(TimeConfiguration timeConfiguration) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ITimeDetectorService {
        @Override // android.app.timedetector.ITimeDetectorService
        public TimeCapabilitiesAndConfig getCapabilitiesAndConfig() throws RemoteException {
            return null;
        }

        @Override // android.app.timedetector.ITimeDetectorService
        public boolean updateConfiguration(TimeConfiguration timeConfiguration) throws RemoteException {
            return false;
        }

        @Override // android.app.timedetector.ITimeDetectorService
        public void suggestExternalTime(ExternalTimeSuggestion timeSuggestion) throws RemoteException {
        }

        @Override // android.app.timedetector.ITimeDetectorService
        public void suggestGnssTime(GnssTimeSuggestion timeSuggestion) throws RemoteException {
        }

        @Override // android.app.timedetector.ITimeDetectorService
        public boolean suggestManualTime(ManualTimeSuggestion timeSuggestion) throws RemoteException {
            return false;
        }

        @Override // android.app.timedetector.ITimeDetectorService
        public void suggestNetworkTime(NetworkTimeSuggestion timeSuggestion) throws RemoteException {
        }

        @Override // android.app.timedetector.ITimeDetectorService
        public void suggestTelephonyTime(TelephonyTimeSuggestion timeSuggestion) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ITimeDetectorService {
        static final int TRANSACTION_getCapabilitiesAndConfig = 1;
        static final int TRANSACTION_suggestExternalTime = 3;
        static final int TRANSACTION_suggestGnssTime = 4;
        static final int TRANSACTION_suggestManualTime = 5;
        static final int TRANSACTION_suggestNetworkTime = 6;
        static final int TRANSACTION_suggestTelephonyTime = 7;
        static final int TRANSACTION_updateConfiguration = 2;

        public Stub() {
            attachInterface(this, ITimeDetectorService.DESCRIPTOR);
        }

        public static ITimeDetectorService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITimeDetectorService.DESCRIPTOR);
            if (iin != null && (iin instanceof ITimeDetectorService)) {
                return (ITimeDetectorService) iin;
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
                    return "getCapabilitiesAndConfig";
                case 2:
                    return "updateConfiguration";
                case 3:
                    return "suggestExternalTime";
                case 4:
                    return "suggestGnssTime";
                case 5:
                    return "suggestManualTime";
                case 6:
                    return "suggestNetworkTime";
                case 7:
                    return "suggestTelephonyTime";
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
                data.enforceInterface(ITimeDetectorService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITimeDetectorService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            TimeCapabilitiesAndConfig _result = getCapabilitiesAndConfig();
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            TimeConfiguration _arg0 = (TimeConfiguration) data.readTypedObject(TimeConfiguration.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result2 = updateConfiguration(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 3:
                            ExternalTimeSuggestion _arg02 = (ExternalTimeSuggestion) data.readTypedObject(ExternalTimeSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            suggestExternalTime(_arg02);
                            reply.writeNoException();
                            break;
                        case 4:
                            GnssTimeSuggestion _arg03 = (GnssTimeSuggestion) data.readTypedObject(GnssTimeSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            suggestGnssTime(_arg03);
                            reply.writeNoException();
                            break;
                        case 5:
                            ManualTimeSuggestion _arg04 = (ManualTimeSuggestion) data.readTypedObject(ManualTimeSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result3 = suggestManualTime(_arg04);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 6:
                            NetworkTimeSuggestion _arg05 = (NetworkTimeSuggestion) data.readTypedObject(NetworkTimeSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            suggestNetworkTime(_arg05);
                            reply.writeNoException();
                            break;
                        case 7:
                            TelephonyTimeSuggestion _arg06 = (TelephonyTimeSuggestion) data.readTypedObject(TelephonyTimeSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            suggestTelephonyTime(_arg06);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ITimeDetectorService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITimeDetectorService.DESCRIPTOR;
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public TimeCapabilitiesAndConfig getCapabilitiesAndConfig() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    TimeCapabilitiesAndConfig _result = (TimeCapabilitiesAndConfig) _reply.readTypedObject(TimeCapabilitiesAndConfig.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public boolean updateConfiguration(TimeConfiguration timeConfiguration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeConfiguration, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public void suggestExternalTime(ExternalTimeSuggestion timeSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeSuggestion, 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public void suggestGnssTime(GnssTimeSuggestion timeSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeSuggestion, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public boolean suggestManualTime(ManualTimeSuggestion timeSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeSuggestion, 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public void suggestNetworkTime(NetworkTimeSuggestion timeSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeSuggestion, 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timedetector.ITimeDetectorService
            public void suggestTelephonyTime(TelephonyTimeSuggestion timeSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeSuggestion, 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
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
