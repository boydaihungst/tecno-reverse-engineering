package android.media;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes2.dex */
public interface IMediaRouter2Manager extends IInterface {
    public static final String DESCRIPTOR = "android.media.IMediaRouter2Manager";

    void notifyDiscoveryPreferenceChanged(String str, RouteDiscoveryPreference routeDiscoveryPreference) throws RemoteException;

    void notifyRequestFailed(int i, int i2) throws RemoteException;

    void notifyRoutesAdded(List<MediaRoute2Info> list) throws RemoteException;

    void notifyRoutesChanged(List<MediaRoute2Info> list) throws RemoteException;

    void notifyRoutesRemoved(List<MediaRoute2Info> list) throws RemoteException;

    void notifySessionCreated(int i, RoutingSessionInfo routingSessionInfo) throws RemoteException;

    void notifySessionReleased(RoutingSessionInfo routingSessionInfo) throws RemoteException;

    void notifySessionUpdated(RoutingSessionInfo routingSessionInfo) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMediaRouter2Manager {
        @Override // android.media.IMediaRouter2Manager
        public void notifySessionCreated(int requestId, RoutingSessionInfo session) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifySessionUpdated(RoutingSessionInfo session) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifySessionReleased(RoutingSessionInfo session) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifyDiscoveryPreferenceChanged(String packageName, RouteDiscoveryPreference discoveryPreference) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifyRoutesAdded(List<MediaRoute2Info> routes) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifyRoutesRemoved(List<MediaRoute2Info> routes) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifyRoutesChanged(List<MediaRoute2Info> routes) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2Manager
        public void notifyRequestFailed(int requestId, int reason) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMediaRouter2Manager {
        static final int TRANSACTION_notifyDiscoveryPreferenceChanged = 4;
        static final int TRANSACTION_notifyRequestFailed = 8;
        static final int TRANSACTION_notifyRoutesAdded = 5;
        static final int TRANSACTION_notifyRoutesChanged = 7;
        static final int TRANSACTION_notifyRoutesRemoved = 6;
        static final int TRANSACTION_notifySessionCreated = 1;
        static final int TRANSACTION_notifySessionReleased = 3;
        static final int TRANSACTION_notifySessionUpdated = 2;

        public Stub() {
            attachInterface(this, IMediaRouter2Manager.DESCRIPTOR);
        }

        public static IMediaRouter2Manager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMediaRouter2Manager.DESCRIPTOR);
            if (iin != null && (iin instanceof IMediaRouter2Manager)) {
                return (IMediaRouter2Manager) iin;
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
                    return "notifySessionCreated";
                case 2:
                    return "notifySessionUpdated";
                case 3:
                    return "notifySessionReleased";
                case 4:
                    return "notifyDiscoveryPreferenceChanged";
                case 5:
                    return "notifyRoutesAdded";
                case 6:
                    return "notifyRoutesRemoved";
                case 7:
                    return "notifyRoutesChanged";
                case 8:
                    return "notifyRequestFailed";
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
                data.enforceInterface(IMediaRouter2Manager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IMediaRouter2Manager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            RoutingSessionInfo _arg1 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifySessionCreated(_arg0, _arg1);
                            break;
                        case 2:
                            RoutingSessionInfo _arg02 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifySessionUpdated(_arg02);
                            break;
                        case 3:
                            RoutingSessionInfo _arg03 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifySessionReleased(_arg03);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            RouteDiscoveryPreference _arg12 = (RouteDiscoveryPreference) data.readTypedObject(RouteDiscoveryPreference.CREATOR);
                            data.enforceNoDataAvail();
                            notifyDiscoveryPreferenceChanged(_arg04, _arg12);
                            break;
                        case 5:
                            List<MediaRoute2Info> _arg05 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRoutesAdded(_arg05);
                            break;
                        case 6:
                            List<MediaRoute2Info> _arg06 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRoutesRemoved(_arg06);
                            break;
                        case 7:
                            List<MediaRoute2Info> _arg07 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRoutesChanged(_arg07);
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyRequestFailed(_arg08, _arg13);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IMediaRouter2Manager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMediaRouter2Manager.DESCRIPTOR;
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifySessionCreated(int requestId, RoutingSessionInfo session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeInt(requestId);
                    _data.writeTypedObject(session, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifySessionUpdated(RoutingSessionInfo session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeTypedObject(session, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifySessionReleased(RoutingSessionInfo session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeTypedObject(session, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifyDiscoveryPreferenceChanged(String packageName, RouteDiscoveryPreference discoveryPreference) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(discoveryPreference, 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifyRoutesAdded(List<MediaRoute2Info> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifyRoutesRemoved(List<MediaRoute2Info> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifyRoutesChanged(List<MediaRoute2Info> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2Manager
            public void notifyRequestFailed(int requestId, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2Manager.DESCRIPTOR);
                    _data.writeInt(requestId);
                    _data.writeInt(reason);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 7;
        }
    }
}
