package android.app.timezonedetector;

import android.app.time.ITimeZoneDetectorListener;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface ITimeZoneDetectorService extends IInterface {
    public static final String DESCRIPTOR = "android.app.timezonedetector.ITimeZoneDetectorService";

    void addListener(ITimeZoneDetectorListener iTimeZoneDetectorListener) throws RemoteException;

    TimeZoneCapabilitiesAndConfig getCapabilitiesAndConfig() throws RemoteException;

    void removeListener(ITimeZoneDetectorListener iTimeZoneDetectorListener) throws RemoteException;

    boolean suggestManualTimeZone(ManualTimeZoneSuggestion manualTimeZoneSuggestion) throws RemoteException;

    void suggestTelephonyTimeZone(TelephonyTimeZoneSuggestion telephonyTimeZoneSuggestion) throws RemoteException;

    boolean updateConfiguration(TimeZoneConfiguration timeZoneConfiguration) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ITimeZoneDetectorService {
        @Override // android.app.timezonedetector.ITimeZoneDetectorService
        public TimeZoneCapabilitiesAndConfig getCapabilitiesAndConfig() throws RemoteException {
            return null;
        }

        @Override // android.app.timezonedetector.ITimeZoneDetectorService
        public void addListener(ITimeZoneDetectorListener listener) throws RemoteException {
        }

        @Override // android.app.timezonedetector.ITimeZoneDetectorService
        public void removeListener(ITimeZoneDetectorListener listener) throws RemoteException {
        }

        @Override // android.app.timezonedetector.ITimeZoneDetectorService
        public boolean updateConfiguration(TimeZoneConfiguration configuration) throws RemoteException {
            return false;
        }

        @Override // android.app.timezonedetector.ITimeZoneDetectorService
        public boolean suggestManualTimeZone(ManualTimeZoneSuggestion timeZoneSuggestion) throws RemoteException {
            return false;
        }

        @Override // android.app.timezonedetector.ITimeZoneDetectorService
        public void suggestTelephonyTimeZone(TelephonyTimeZoneSuggestion timeZoneSuggestion) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ITimeZoneDetectorService {
        static final int TRANSACTION_addListener = 2;
        static final int TRANSACTION_getCapabilitiesAndConfig = 1;
        static final int TRANSACTION_removeListener = 3;
        static final int TRANSACTION_suggestManualTimeZone = 5;
        static final int TRANSACTION_suggestTelephonyTimeZone = 6;
        static final int TRANSACTION_updateConfiguration = 4;

        public Stub() {
            attachInterface(this, ITimeZoneDetectorService.DESCRIPTOR);
        }

        public static ITimeZoneDetectorService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITimeZoneDetectorService.DESCRIPTOR);
            if (iin != null && (iin instanceof ITimeZoneDetectorService)) {
                return (ITimeZoneDetectorService) iin;
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
                    return "addListener";
                case 3:
                    return "removeListener";
                case 4:
                    return "updateConfiguration";
                case 5:
                    return "suggestManualTimeZone";
                case 6:
                    return "suggestTelephonyTimeZone";
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
                data.enforceInterface(ITimeZoneDetectorService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITimeZoneDetectorService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            TimeZoneCapabilitiesAndConfig _result = getCapabilitiesAndConfig();
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            ITimeZoneDetectorListener _arg0 = ITimeZoneDetectorListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addListener(_arg0);
                            reply.writeNoException();
                            break;
                        case 3:
                            ITimeZoneDetectorListener _arg02 = ITimeZoneDetectorListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeListener(_arg02);
                            reply.writeNoException();
                            break;
                        case 4:
                            TimeZoneConfiguration _arg03 = (TimeZoneConfiguration) data.readTypedObject(TimeZoneConfiguration.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result2 = updateConfiguration(_arg03);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 5:
                            ManualTimeZoneSuggestion _arg04 = (ManualTimeZoneSuggestion) data.readTypedObject(ManualTimeZoneSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result3 = suggestManualTimeZone(_arg04);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 6:
                            TelephonyTimeZoneSuggestion _arg05 = (TelephonyTimeZoneSuggestion) data.readTypedObject(TelephonyTimeZoneSuggestion.CREATOR);
                            data.enforceNoDataAvail();
                            suggestTelephonyTimeZone(_arg05);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ITimeZoneDetectorService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITimeZoneDetectorService.DESCRIPTOR;
            }

            @Override // android.app.timezonedetector.ITimeZoneDetectorService
            public TimeZoneCapabilitiesAndConfig getCapabilitiesAndConfig() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeZoneDetectorService.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    TimeZoneCapabilitiesAndConfig _result = (TimeZoneCapabilitiesAndConfig) _reply.readTypedObject(TimeZoneCapabilitiesAndConfig.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezonedetector.ITimeZoneDetectorService
            public void addListener(ITimeZoneDetectorListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeZoneDetectorService.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezonedetector.ITimeZoneDetectorService
            public void removeListener(ITimeZoneDetectorListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeZoneDetectorService.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezonedetector.ITimeZoneDetectorService
            public boolean updateConfiguration(TimeZoneConfiguration configuration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeZoneDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(configuration, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezonedetector.ITimeZoneDetectorService
            public boolean suggestManualTimeZone(ManualTimeZoneSuggestion timeZoneSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeZoneDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeZoneSuggestion, 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.app.timezonedetector.ITimeZoneDetectorService
            public void suggestTelephonyTimeZone(TelephonyTimeZoneSuggestion timeZoneSuggestion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITimeZoneDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(timeZoneSuggestion, 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 5;
        }
    }
}
