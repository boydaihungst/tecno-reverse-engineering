package android.content.pm;

import android.content.pm.IPackageChangeObserver;
import android.content.pm.IStagedApexObserver;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IPackageManagerNative extends IInterface {
    public static final int LOCATION_PRODUCT = 4;
    public static final int LOCATION_SYSTEM = 1;
    public static final int LOCATION_VENDOR = 2;

    String[] getAllPackages() throws RemoteException;

    String getInstallerForPackage(String str) throws RemoteException;

    int getLocationFlags(String str) throws RemoteException;

    String getModuleMetadataPackageName() throws RemoteException;

    String[] getNamesForUids(int[] iArr) throws RemoteException;

    StagedApexInfo getStagedApexInfo(String str) throws RemoteException;

    String[] getStagedApexModuleNames() throws RemoteException;

    int getTargetSdkVersionForPackage(String str) throws RemoteException;

    long getVersionCodeForPackage(String str) throws RemoteException;

    boolean hasSha256SigningCertificate(String str, byte[] bArr) throws RemoteException;

    boolean hasSystemFeature(String str, int i) throws RemoteException;

    boolean[] isAudioPlaybackCaptureAllowed(String[] strArr) throws RemoteException;

    boolean isPackageDebuggable(String str) throws RemoteException;

    void registerPackageChangeObserver(IPackageChangeObserver iPackageChangeObserver) throws RemoteException;

    void registerStagedApexObserver(IStagedApexObserver iStagedApexObserver) throws RemoteException;

    void unregisterPackageChangeObserver(IPackageChangeObserver iPackageChangeObserver) throws RemoteException;

    void unregisterStagedApexObserver(IStagedApexObserver iStagedApexObserver) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IPackageManagerNative {
        @Override // android.content.pm.IPackageManagerNative
        public String[] getNamesForUids(int[] uids) throws RemoteException {
            return null;
        }

        @Override // android.content.pm.IPackageManagerNative
        public String getInstallerForPackage(String packageName) throws RemoteException {
            return null;
        }

        @Override // android.content.pm.IPackageManagerNative
        public long getVersionCodeForPackage(String packageName) throws RemoteException {
            return 0L;
        }

        @Override // android.content.pm.IPackageManagerNative
        public boolean[] isAudioPlaybackCaptureAllowed(String[] packageNames) throws RemoteException {
            return null;
        }

        @Override // android.content.pm.IPackageManagerNative
        public int getLocationFlags(String packageName) throws RemoteException {
            return 0;
        }

        @Override // android.content.pm.IPackageManagerNative
        public int getTargetSdkVersionForPackage(String packageName) throws RemoteException {
            return 0;
        }

        @Override // android.content.pm.IPackageManagerNative
        public String getModuleMetadataPackageName() throws RemoteException {
            return null;
        }

        @Override // android.content.pm.IPackageManagerNative
        public String[] getAllPackages() throws RemoteException {
            return null;
        }

        @Override // android.content.pm.IPackageManagerNative
        public void registerPackageChangeObserver(IPackageChangeObserver observer) throws RemoteException {
        }

        @Override // android.content.pm.IPackageManagerNative
        public void unregisterPackageChangeObserver(IPackageChangeObserver observer) throws RemoteException {
        }

        @Override // android.content.pm.IPackageManagerNative
        public boolean hasSha256SigningCertificate(String packageName, byte[] certificate) throws RemoteException {
            return false;
        }

        @Override // android.content.pm.IPackageManagerNative
        public boolean isPackageDebuggable(String packageName) throws RemoteException {
            return false;
        }

        @Override // android.content.pm.IPackageManagerNative
        public boolean hasSystemFeature(String featureName, int version) throws RemoteException {
            return false;
        }

        @Override // android.content.pm.IPackageManagerNative
        public void registerStagedApexObserver(IStagedApexObserver observer) throws RemoteException {
        }

        @Override // android.content.pm.IPackageManagerNative
        public void unregisterStagedApexObserver(IStagedApexObserver observer) throws RemoteException {
        }

        @Override // android.content.pm.IPackageManagerNative
        public String[] getStagedApexModuleNames() throws RemoteException {
            return null;
        }

        @Override // android.content.pm.IPackageManagerNative
        public StagedApexInfo getStagedApexInfo(String moduleName) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IPackageManagerNative {
        public static final String DESCRIPTOR = "android$content$pm$IPackageManagerNative".replace('$', '.');
        static final int TRANSACTION_getAllPackages = 8;
        static final int TRANSACTION_getInstallerForPackage = 2;
        static final int TRANSACTION_getLocationFlags = 5;
        static final int TRANSACTION_getModuleMetadataPackageName = 7;
        static final int TRANSACTION_getNamesForUids = 1;
        static final int TRANSACTION_getStagedApexInfo = 17;
        static final int TRANSACTION_getStagedApexModuleNames = 16;
        static final int TRANSACTION_getTargetSdkVersionForPackage = 6;
        static final int TRANSACTION_getVersionCodeForPackage = 3;
        static final int TRANSACTION_hasSha256SigningCertificate = 11;
        static final int TRANSACTION_hasSystemFeature = 13;
        static final int TRANSACTION_isAudioPlaybackCaptureAllowed = 4;
        static final int TRANSACTION_isPackageDebuggable = 12;
        static final int TRANSACTION_registerPackageChangeObserver = 9;
        static final int TRANSACTION_registerStagedApexObserver = 14;
        static final int TRANSACTION_unregisterPackageChangeObserver = 10;
        static final int TRANSACTION_unregisterStagedApexObserver = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManagerNative asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPackageManagerNative)) {
                return (IPackageManagerNative) iin;
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
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int[] _arg0 = data.createIntArray();
                            data.enforceNoDataAvail();
                            String[] _result = getNamesForUids(_arg0);
                            reply.writeNoException();
                            reply.writeStringArray(_result);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            String _result2 = getInstallerForPackage(_arg02);
                            reply.writeNoException();
                            reply.writeString(_result2);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            data.enforceNoDataAvail();
                            long _result3 = getVersionCodeForPackage(_arg03);
                            reply.writeNoException();
                            reply.writeLong(_result3);
                            break;
                        case 4:
                            String[] _arg04 = data.createStringArray();
                            data.enforceNoDataAvail();
                            boolean[] _result4 = isAudioPlaybackCaptureAllowed(_arg04);
                            reply.writeNoException();
                            reply.writeBooleanArray(_result4);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            data.enforceNoDataAvail();
                            int _result5 = getLocationFlags(_arg05);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            data.enforceNoDataAvail();
                            int _result6 = getTargetSdkVersionForPackage(_arg06);
                            reply.writeNoException();
                            reply.writeInt(_result6);
                            break;
                        case 7:
                            String _result7 = getModuleMetadataPackageName();
                            reply.writeNoException();
                            reply.writeString(_result7);
                            break;
                        case 8:
                            String[] _result8 = getAllPackages();
                            reply.writeNoException();
                            reply.writeStringArray(_result8);
                            break;
                        case 9:
                            IPackageChangeObserver _arg07 = IPackageChangeObserver.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerPackageChangeObserver(_arg07);
                            reply.writeNoException();
                            break;
                        case 10:
                            IPackageChangeObserver _arg08 = IPackageChangeObserver.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterPackageChangeObserver(_arg08);
                            reply.writeNoException();
                            break;
                        case 11:
                            String _arg09 = data.readString();
                            byte[] _arg1 = data.createByteArray();
                            data.enforceNoDataAvail();
                            boolean _result9 = hasSha256SigningCertificate(_arg09, _arg1);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 12:
                            String _arg010 = data.readString();
                            data.enforceNoDataAvail();
                            boolean _result10 = isPackageDebuggable(_arg010);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 13:
                            String _arg011 = data.readString();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result11 = hasSystemFeature(_arg011, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 14:
                            IStagedApexObserver _arg012 = IStagedApexObserver.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            registerStagedApexObserver(_arg012);
                            reply.writeNoException();
                            break;
                        case 15:
                            IStagedApexObserver _arg013 = IStagedApexObserver.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterStagedApexObserver(_arg013);
                            reply.writeNoException();
                            break;
                        case 16:
                            String[] _result12 = getStagedApexModuleNames();
                            reply.writeNoException();
                            reply.writeStringArray(_result12);
                            break;
                        case 17:
                            String _arg014 = data.readString();
                            data.enforceNoDataAvail();
                            StagedApexInfo _result13 = getStagedApexInfo(_arg014);
                            reply.writeNoException();
                            reply.writeTypedObject(_result13, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IPackageManagerNative {
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

            @Override // android.content.pm.IPackageManagerNative
            public String[] getNamesForUids(int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public String getInstallerForPackage(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public long getVersionCodeForPackage(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public boolean[] isAudioPlaybackCaptureAllowed(String[] packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(packageNames);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean[] _result = _reply.createBooleanArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public int getLocationFlags(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public int getTargetSdkVersionForPackage(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public String getModuleMetadataPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public String[] getAllPackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public void registerPackageChangeObserver(IPackageChangeObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(observer);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public void unregisterPackageChangeObserver(IPackageChangeObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(observer);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public boolean hasSha256SigningCertificate(String packageName, byte[] certificate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeByteArray(certificate);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public boolean isPackageDebuggable(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public boolean hasSystemFeature(String featureName, int version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(featureName);
                    _data.writeInt(version);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public void registerStagedApexObserver(IStagedApexObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(observer);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public void unregisterStagedApexObserver(IStagedApexObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(observer);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public String[] getStagedApexModuleNames() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.content.pm.IPackageManagerNative
            public StagedApexInfo getStagedApexInfo(String moduleName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(moduleName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    StagedApexInfo _result = (StagedApexInfo) _reply.readTypedObject(StagedApexInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
