package android.companion;

import android.app.PendingIntent;
import android.companion.IAssociationRequestCallback;
import android.companion.IOnAssociationsChangedListener;
import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes.dex */
public interface ICompanionDeviceManager extends IInterface {
    void addOnAssociationsChangedListener(IOnAssociationsChangedListener iOnAssociationsChangedListener, int i) throws RemoteException;

    void associate(AssociationRequest associationRequest, IAssociationRequestCallback iAssociationRequestCallback, String str, int i) throws RemoteException;

    @Deprecated
    boolean canPairWithoutPrompt(String str, String str2, int i) throws RemoteException;

    @Deprecated
    void createAssociation(String str, String str2, int i, byte[] bArr) throws RemoteException;

    void disassociate(int i) throws RemoteException;

    void dispatchMessage(int i, int i2, byte[] bArr) throws RemoteException;

    List<AssociationInfo> getAllAssociationsForUser(int i) throws RemoteException;

    List<AssociationInfo> getAssociations(String str, int i) throws RemoteException;

    @Deprecated
    boolean hasNotificationAccess(ComponentName componentName) throws RemoteException;

    @Deprecated
    boolean isDeviceAssociatedForWifiConnection(String str, String str2, int i) throws RemoteException;

    @Deprecated
    void legacyDisassociate(String str, String str2, int i) throws RemoteException;

    void notifyDeviceAppeared(int i) throws RemoteException;

    void notifyDeviceDisappeared(int i) throws RemoteException;

    void registerDevicePresenceListenerService(String str, String str2, int i) throws RemoteException;

    void removeOnAssociationsChangedListener(IOnAssociationsChangedListener iOnAssociationsChangedListener, int i) throws RemoteException;

    PendingIntent requestNotificationAccess(ComponentName componentName, int i) throws RemoteException;

    void unregisterDevicePresenceListenerService(String str, String str2, int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICompanionDeviceManager {
        @Override // android.companion.ICompanionDeviceManager
        public void associate(AssociationRequest request, IAssociationRequestCallback callback, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public List<AssociationInfo> getAssociations(String callingPackage, int userId) throws RemoteException {
            return null;
        }

        @Override // android.companion.ICompanionDeviceManager
        public List<AssociationInfo> getAllAssociationsForUser(int userId) throws RemoteException {
            return null;
        }

        @Override // android.companion.ICompanionDeviceManager
        public void legacyDisassociate(String deviceMacAddress, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void disassociate(int associationId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public boolean hasNotificationAccess(ComponentName component) throws RemoteException {
            return false;
        }

        @Override // android.companion.ICompanionDeviceManager
        public PendingIntent requestNotificationAccess(ComponentName component, int userId) throws RemoteException {
            return null;
        }

        @Override // android.companion.ICompanionDeviceManager
        public boolean isDeviceAssociatedForWifiConnection(String packageName, String macAddress, int userId) throws RemoteException {
            return false;
        }

        @Override // android.companion.ICompanionDeviceManager
        public void registerDevicePresenceListenerService(String deviceAddress, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void unregisterDevicePresenceListenerService(String deviceAddress, String callingPackage, int userId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public boolean canPairWithoutPrompt(String packageName, String deviceMacAddress, int userId) throws RemoteException {
            return false;
        }

        @Override // android.companion.ICompanionDeviceManager
        public void createAssociation(String packageName, String macAddress, int userId, byte[] certificate) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void dispatchMessage(int messageId, int associationId, byte[] message) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void addOnAssociationsChangedListener(IOnAssociationsChangedListener listener, int userId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void removeOnAssociationsChangedListener(IOnAssociationsChangedListener listener, int userId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void notifyDeviceAppeared(int associationId) throws RemoteException {
        }

        @Override // android.companion.ICompanionDeviceManager
        public void notifyDeviceDisappeared(int associationId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICompanionDeviceManager {
        public static final String DESCRIPTOR = "android.companion.ICompanionDeviceManager";
        static final int TRANSACTION_addOnAssociationsChangedListener = 14;
        static final int TRANSACTION_associate = 1;
        static final int TRANSACTION_canPairWithoutPrompt = 11;
        static final int TRANSACTION_createAssociation = 12;
        static final int TRANSACTION_disassociate = 5;
        static final int TRANSACTION_dispatchMessage = 13;
        static final int TRANSACTION_getAllAssociationsForUser = 3;
        static final int TRANSACTION_getAssociations = 2;
        static final int TRANSACTION_hasNotificationAccess = 6;
        static final int TRANSACTION_isDeviceAssociatedForWifiConnection = 8;
        static final int TRANSACTION_legacyDisassociate = 4;
        static final int TRANSACTION_notifyDeviceAppeared = 16;
        static final int TRANSACTION_notifyDeviceDisappeared = 17;
        static final int TRANSACTION_registerDevicePresenceListenerService = 9;
        static final int TRANSACTION_removeOnAssociationsChangedListener = 15;
        static final int TRANSACTION_requestNotificationAccess = 7;
        static final int TRANSACTION_unregisterDevicePresenceListenerService = 10;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ICompanionDeviceManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ICompanionDeviceManager)) {
                return (ICompanionDeviceManager) iin;
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
                    return "associate";
                case 2:
                    return "getAssociations";
                case 3:
                    return "getAllAssociationsForUser";
                case 4:
                    return "legacyDisassociate";
                case 5:
                    return "disassociate";
                case 6:
                    return "hasNotificationAccess";
                case 7:
                    return "requestNotificationAccess";
                case 8:
                    return "isDeviceAssociatedForWifiConnection";
                case 9:
                    return "registerDevicePresenceListenerService";
                case 10:
                    return "unregisterDevicePresenceListenerService";
                case 11:
                    return "canPairWithoutPrompt";
                case 12:
                    return "createAssociation";
                case 13:
                    return "dispatchMessage";
                case 14:
                    return "addOnAssociationsChangedListener";
                case 15:
                    return "removeOnAssociationsChangedListener";
                case 16:
                    return "notifyDeviceAppeared";
                case 17:
                    return "notifyDeviceDisappeared";
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
                            AssociationRequest _arg0 = (AssociationRequest) data.readTypedObject(AssociationRequest.CREATOR);
                            IAssociationRequestCallback _arg1 = IAssociationRequestCallback.Stub.asInterface(data.readStrongBinder());
                            String _arg2 = data.readString();
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            associate(_arg0, _arg1, _arg2, _arg3);
                            reply.writeNoException();
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            List<AssociationInfo> _result = getAssociations(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeTypedList(_result);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            List<AssociationInfo> _result2 = getAllAssociationsForUser(_arg03);
                            reply.writeNoException();
                            reply.writeTypedList(_result2);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg13 = data.readString();
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            legacyDisassociate(_arg04, _arg13, _arg22);
                            reply.writeNoException();
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            disassociate(_arg05);
                            reply.writeNoException();
                            break;
                        case 6:
                            ComponentName _arg06 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result3 = hasNotificationAccess(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 7:
                            ComponentName _arg07 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            PendingIntent _result4 = requestNotificationAccess(_arg07, _arg14);
                            reply.writeNoException();
                            reply.writeTypedObject(_result4, 1);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            String _arg15 = data.readString();
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result5 = isDeviceAssociatedForWifiConnection(_arg08, _arg15, _arg23);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            String _arg16 = data.readString();
                            int _arg24 = data.readInt();
                            data.enforceNoDataAvail();
                            registerDevicePresenceListenerService(_arg09, _arg16, _arg24);
                            reply.writeNoException();
                            break;
                        case 10:
                            String _arg010 = data.readString();
                            String _arg17 = data.readString();
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            unregisterDevicePresenceListenerService(_arg010, _arg17, _arg25);
                            reply.writeNoException();
                            break;
                        case 11:
                            String _arg011 = data.readString();
                            String _arg18 = data.readString();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result6 = canPairWithoutPrompt(_arg011, _arg18, _arg26);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 12:
                            String _arg012 = data.readString();
                            String _arg19 = data.readString();
                            int _arg27 = data.readInt();
                            byte[] _arg32 = data.createByteArray();
                            data.enforceNoDataAvail();
                            createAssociation(_arg012, _arg19, _arg27, _arg32);
                            reply.writeNoException();
                            break;
                        case 13:
                            int _arg013 = data.readInt();
                            int _arg110 = data.readInt();
                            byte[] _arg28 = data.createByteArray();
                            data.enforceNoDataAvail();
                            dispatchMessage(_arg013, _arg110, _arg28);
                            reply.writeNoException();
                            break;
                        case 14:
                            IOnAssociationsChangedListener _arg014 = IOnAssociationsChangedListener.Stub.asInterface(data.readStrongBinder());
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            addOnAssociationsChangedListener(_arg014, _arg111);
                            reply.writeNoException();
                            break;
                        case 15:
                            IOnAssociationsChangedListener _arg015 = IOnAssociationsChangedListener.Stub.asInterface(data.readStrongBinder());
                            int _arg112 = data.readInt();
                            data.enforceNoDataAvail();
                            removeOnAssociationsChangedListener(_arg015, _arg112);
                            reply.writeNoException();
                            break;
                        case 16:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyDeviceAppeared(_arg016);
                            reply.writeNoException();
                            break;
                        case 17:
                            int _arg017 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyDeviceDisappeared(_arg017);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements ICompanionDeviceManager {
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

            @Override // android.companion.ICompanionDeviceManager
            public void associate(AssociationRequest request, IAssociationRequestCallback callback, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    _data.writeStrongInterface(callback);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public List<AssociationInfo> getAssociations(String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<AssociationInfo> _result = _reply.createTypedArrayList(AssociationInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public List<AssociationInfo> getAllAssociationsForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    List<AssociationInfo> _result = _reply.createTypedArrayList(AssociationInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void legacyDisassociate(String deviceMacAddress, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceMacAddress);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void disassociate(int associationId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(associationId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public boolean hasNotificationAccess(ComponentName component) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(component, 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public PendingIntent requestNotificationAccess(ComponentName component, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(component, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    PendingIntent _result = (PendingIntent) _reply.readTypedObject(PendingIntent.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public boolean isDeviceAssociatedForWifiConnection(String packageName, String macAddress, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(macAddress);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void registerDevicePresenceListenerService(String deviceAddress, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceAddress);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void unregisterDevicePresenceListenerService(String deviceAddress, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(deviceAddress);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public boolean canPairWithoutPrompt(String packageName, String deviceMacAddress, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(deviceMacAddress);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void createAssociation(String packageName, String macAddress, int userId, byte[] certificate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(macAddress);
                    _data.writeInt(userId);
                    _data.writeByteArray(certificate);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void dispatchMessage(int messageId, int associationId, byte[] message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(messageId);
                    _data.writeInt(associationId);
                    _data.writeByteArray(message);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void addOnAssociationsChangedListener(IOnAssociationsChangedListener listener, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    _data.writeInt(userId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void removeOnAssociationsChangedListener(IOnAssociationsChangedListener listener, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    _data.writeInt(userId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void notifyDeviceAppeared(int associationId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(associationId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.companion.ICompanionDeviceManager
            public void notifyDeviceDisappeared(int associationId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(associationId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 16;
        }
    }
}
