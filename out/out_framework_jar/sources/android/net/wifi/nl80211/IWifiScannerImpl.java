package android.net.wifi.nl80211;

import android.net.wifi.nl80211.IPnoScanEvent;
import android.net.wifi.nl80211.IScanEvent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IWifiScannerImpl extends IInterface {
    public static final String DESCRIPTOR = "android.net.wifi.nl80211.IWifiScannerImpl";
    public static final int SCAN_TYPE_DEFAULT = -1;
    public static final int SCAN_TYPE_HIGH_ACCURACY = 2;
    public static final int SCAN_TYPE_LOW_POWER = 1;
    public static final int SCAN_TYPE_LOW_SPAN = 0;

    void abortScan() throws RemoteException;

    int getMaxSsidsPerScan() throws RemoteException;

    NativeScanResult[] getPnoScanResults() throws RemoteException;

    NativeScanResult[] getScanResults() throws RemoteException;

    boolean scan(SingleScanSettings singleScanSettings) throws RemoteException;

    boolean startPnoScan(PnoSettings pnoSettings) throws RemoteException;

    boolean stopPnoScan() throws RemoteException;

    void subscribePnoScanEvents(IPnoScanEvent iPnoScanEvent) throws RemoteException;

    void subscribeScanEvents(IScanEvent iScanEvent) throws RemoteException;

    void unsubscribePnoScanEvents() throws RemoteException;

    void unsubscribeScanEvents() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IWifiScannerImpl {
        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public NativeScanResult[] getScanResults() throws RemoteException {
            return null;
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public NativeScanResult[] getPnoScanResults() throws RemoteException {
            return null;
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public int getMaxSsidsPerScan() throws RemoteException {
            return 0;
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public boolean scan(SingleScanSettings scanSettings) throws RemoteException {
            return false;
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public void subscribeScanEvents(IScanEvent handler) throws RemoteException {
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public void unsubscribeScanEvents() throws RemoteException {
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public void subscribePnoScanEvents(IPnoScanEvent handler) throws RemoteException {
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public void unsubscribePnoScanEvents() throws RemoteException {
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public boolean startPnoScan(PnoSettings pnoSettings) throws RemoteException {
            return false;
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public boolean stopPnoScan() throws RemoteException {
            return false;
        }

        @Override // android.net.wifi.nl80211.IWifiScannerImpl
        public void abortScan() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IWifiScannerImpl {
        static final int TRANSACTION_abortScan = 11;
        static final int TRANSACTION_getMaxSsidsPerScan = 3;
        static final int TRANSACTION_getPnoScanResults = 2;
        static final int TRANSACTION_getScanResults = 1;
        static final int TRANSACTION_scan = 4;
        static final int TRANSACTION_startPnoScan = 9;
        static final int TRANSACTION_stopPnoScan = 10;
        static final int TRANSACTION_subscribePnoScanEvents = 7;
        static final int TRANSACTION_subscribeScanEvents = 5;
        static final int TRANSACTION_unsubscribePnoScanEvents = 8;
        static final int TRANSACTION_unsubscribeScanEvents = 6;

        public Stub() {
            attachInterface(this, IWifiScannerImpl.DESCRIPTOR);
        }

        public static IWifiScannerImpl asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IWifiScannerImpl.DESCRIPTOR);
            if (iin != null && (iin instanceof IWifiScannerImpl)) {
                return (IWifiScannerImpl) iin;
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
                    return "getScanResults";
                case 2:
                    return "getPnoScanResults";
                case 3:
                    return "getMaxSsidsPerScan";
                case 4:
                    return "scan";
                case 5:
                    return "subscribeScanEvents";
                case 6:
                    return "unsubscribeScanEvents";
                case 7:
                    return "subscribePnoScanEvents";
                case 8:
                    return "unsubscribePnoScanEvents";
                case 9:
                    return "startPnoScan";
                case 10:
                    return "stopPnoScan";
                case 11:
                    return "abortScan";
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
                data.enforceInterface(IWifiScannerImpl.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IWifiScannerImpl.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            NativeScanResult[] _result = getScanResults();
                            reply.writeNoException();
                            reply.writeTypedArray(_result, 1);
                            break;
                        case 2:
                            NativeScanResult[] _result2 = getPnoScanResults();
                            reply.writeNoException();
                            reply.writeTypedArray(_result2, 1);
                            break;
                        case 3:
                            int _result3 = getMaxSsidsPerScan();
                            reply.writeNoException();
                            reply.writeInt(_result3);
                            break;
                        case 4:
                            SingleScanSettings _arg0 = (SingleScanSettings) data.readTypedObject(SingleScanSettings.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result4 = scan(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 5:
                            IScanEvent _arg02 = IScanEvent.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            subscribeScanEvents(_arg02);
                            break;
                        case 6:
                            unsubscribeScanEvents();
                            break;
                        case 7:
                            IPnoScanEvent _arg03 = IPnoScanEvent.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            subscribePnoScanEvents(_arg03);
                            break;
                        case 8:
                            unsubscribePnoScanEvents();
                            break;
                        case 9:
                            PnoSettings _arg04 = (PnoSettings) data.readTypedObject(PnoSettings.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result5 = startPnoScan(_arg04);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 10:
                            boolean _result6 = stopPnoScan();
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 11:
                            abortScan();
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IWifiScannerImpl {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IWifiScannerImpl.DESCRIPTOR;
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public NativeScanResult[] getScanResults() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    NativeScanResult[] _result = (NativeScanResult[]) _reply.createTypedArray(NativeScanResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public NativeScanResult[] getPnoScanResults() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    NativeScanResult[] _result = (NativeScanResult[]) _reply.createTypedArray(NativeScanResult.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public int getMaxSsidsPerScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public boolean scan(SingleScanSettings scanSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    _data.writeTypedObject(scanSettings, 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public void subscribeScanEvents(IScanEvent handler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    _data.writeStrongInterface(handler);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public void unsubscribeScanEvents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public void subscribePnoScanEvents(IPnoScanEvent handler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    _data.writeStrongInterface(handler);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public void unsubscribePnoScanEvents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public boolean startPnoScan(PnoSettings pnoSettings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    _data.writeTypedObject(pnoSettings, 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public boolean stopPnoScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.net.wifi.nl80211.IWifiScannerImpl
            public void abortScan() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IWifiScannerImpl.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 10;
        }
    }
}
