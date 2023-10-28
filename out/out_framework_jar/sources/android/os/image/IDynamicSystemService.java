package android.os.image;

import android.Manifest;
import android.app.ActivityThread;
import android.content.AttributionSource;
import android.content.Context;
import android.content.PermissionChecker;
import android.gsi.AvbPublicKey;
import android.gsi.GsiProgress;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface IDynamicSystemService extends IInterface {
    public static final String DESCRIPTOR = "android.os.image.IDynamicSystemService";

    boolean abort() throws RemoteException;

    boolean closePartition() throws RemoteException;

    int createPartition(String str, long j, boolean z) throws RemoteException;

    boolean finishInstallation() throws RemoteException;

    boolean getAvbPublicKey(AvbPublicKey avbPublicKey) throws RemoteException;

    GsiProgress getInstallationProgress() throws RemoteException;

    boolean isEnabled() throws RemoteException;

    boolean isInUse() throws RemoteException;

    boolean isInstalled() throws RemoteException;

    boolean remove() throws RemoteException;

    boolean setAshmem(ParcelFileDescriptor parcelFileDescriptor, long j) throws RemoteException;

    boolean setEnable(boolean z, boolean z2) throws RemoteException;

    boolean startInstallation(String str) throws RemoteException;

    boolean submitFromAshmem(long j) throws RemoteException;

    long suggestScratchSize() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IDynamicSystemService {
        @Override // android.os.image.IDynamicSystemService
        public boolean startInstallation(String dsuSlot) throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public int createPartition(String name, long size, boolean readOnly) throws RemoteException {
            return 0;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean closePartition() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean finishInstallation() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public GsiProgress getInstallationProgress() throws RemoteException {
            return null;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean abort() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean isInUse() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean isInstalled() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean isEnabled() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean remove() throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean setEnable(boolean enable, boolean oneShot) throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean setAshmem(ParcelFileDescriptor fd, long size) throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean submitFromAshmem(long bytes) throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public boolean getAvbPublicKey(AvbPublicKey dst) throws RemoteException {
            return false;
        }

        @Override // android.os.image.IDynamicSystemService
        public long suggestScratchSize() throws RemoteException {
            return 0L;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IDynamicSystemService {
        static final int TRANSACTION_abort = 6;
        static final int TRANSACTION_closePartition = 3;
        static final int TRANSACTION_createPartition = 2;
        static final int TRANSACTION_finishInstallation = 4;
        static final int TRANSACTION_getAvbPublicKey = 14;
        static final int TRANSACTION_getInstallationProgress = 5;
        static final int TRANSACTION_isEnabled = 9;
        static final int TRANSACTION_isInUse = 7;
        static final int TRANSACTION_isInstalled = 8;
        static final int TRANSACTION_remove = 10;
        static final int TRANSACTION_setAshmem = 12;
        static final int TRANSACTION_setEnable = 11;
        static final int TRANSACTION_startInstallation = 1;
        static final int TRANSACTION_submitFromAshmem = 13;
        static final int TRANSACTION_suggestScratchSize = 15;

        public Stub() {
            attachInterface(this, IDynamicSystemService.DESCRIPTOR);
        }

        public static IDynamicSystemService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IDynamicSystemService.DESCRIPTOR);
            if (iin != null && (iin instanceof IDynamicSystemService)) {
                return (IDynamicSystemService) iin;
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
                    return "startInstallation";
                case 2:
                    return "createPartition";
                case 3:
                    return "closePartition";
                case 4:
                    return "finishInstallation";
                case 5:
                    return "getInstallationProgress";
                case 6:
                    return "abort";
                case 7:
                    return "isInUse";
                case 8:
                    return "isInstalled";
                case 9:
                    return "isEnabled";
                case 10:
                    return "remove";
                case 11:
                    return "setEnable";
                case 12:
                    return "setAshmem";
                case 13:
                    return "submitFromAshmem";
                case 14:
                    return "getAvbPublicKey";
                case 15:
                    return "suggestScratchSize";
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
                data.enforceInterface(IDynamicSystemService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IDynamicSystemService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result = startInstallation(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            long _arg1 = data.readLong();
                            boolean _arg2 = data.readBoolean();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            int _result2 = createPartition(_arg02, _arg1, _arg2);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 3:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result3 = closePartition();
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 4:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result4 = finishInstallation();
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 5:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            GsiProgress _result5 = getInstallationProgress();
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 6:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result6 = abort();
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 7:
                            boolean _result7 = isInUse();
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 8:
                            boolean _result8 = isInstalled();
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 9:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result9 = isEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 10:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result10 = remove();
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 11:
                            boolean _arg03 = data.readBoolean();
                            boolean _arg12 = data.readBoolean();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result11 = setEnable(_arg03, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 12:
                            ParcelFileDescriptor _arg04 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            long _arg13 = data.readLong();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result12 = setAshmem(_arg04, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            break;
                        case 13:
                            long _arg05 = data.readLong();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result13 = submitFromAshmem(_arg05);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case 14:
                            AvbPublicKey _arg06 = new AvbPublicKey();
                            data.enforceNoDataAvail();
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            boolean _result14 = getAvbPublicKey(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            reply.writeTypedObject(_arg06, 1);
                            break;
                        case 15:
                            if (!permissionCheckerWrapper(Manifest.permission.MANAGE_DYNAMIC_SYSTEM, getCallingPid(), new AttributionSource(getCallingUid(), null, null))) {
                                throw new SecurityException("Access denied, requires: android.Manifest.permission.MANAGE_DYNAMIC_SYSTEM");
                            }
                            long _result15 = suggestScratchSize();
                            reply.writeNoException();
                            reply.writeLong(_result15);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IDynamicSystemService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IDynamicSystemService.DESCRIPTOR;
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean startInstallation(String dsuSlot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    _data.writeString(dsuSlot);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public int createPartition(String name, long size, boolean readOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeLong(size);
                    _data.writeBoolean(readOnly);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean closePartition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean finishInstallation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public GsiProgress getInstallationProgress() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    GsiProgress _result = (GsiProgress) _reply.readTypedObject(GsiProgress.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean abort() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean isInUse() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean isInstalled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean isEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean remove() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean setEnable(boolean enable, boolean oneShot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    _data.writeBoolean(oneShot);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean setAshmem(ParcelFileDescriptor fd, long size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    _data.writeTypedObject(fd, 0);
                    _data.writeLong(size);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean submitFromAshmem(long bytes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    _data.writeLong(bytes);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public boolean getAvbPublicKey(AvbPublicKey dst) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    if (_reply.readInt() != 0) {
                        dst.readFromParcel(_reply);
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.os.image.IDynamicSystemService
            public long suggestScratchSize() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IDynamicSystemService.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        private boolean permissionCheckerWrapper(String permission, int pid, AttributionSource attributionSource) {
            Context ctx = ActivityThread.currentActivityThread().getSystemContext();
            return PermissionChecker.checkPermissionForDataDelivery(ctx, permission, pid, attributionSource, "") == 0;
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 14;
        }
    }
}
