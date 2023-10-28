package android.media;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes2.dex */
public interface IMediaRouter2 extends IInterface {
    public static final String DESCRIPTOR = "android.media.IMediaRouter2";

    void notifyRouterRegistered(List<MediaRoute2Info> list, RoutingSessionInfo routingSessionInfo) throws RemoteException;

    void notifyRoutesAdded(List<MediaRoute2Info> list) throws RemoteException;

    void notifyRoutesChanged(List<MediaRoute2Info> list) throws RemoteException;

    void notifyRoutesRemoved(List<MediaRoute2Info> list) throws RemoteException;

    void notifySessionCreated(int i, RoutingSessionInfo routingSessionInfo) throws RemoteException;

    void notifySessionInfoChanged(RoutingSessionInfo routingSessionInfo) throws RemoteException;

    void notifySessionReleased(RoutingSessionInfo routingSessionInfo) throws RemoteException;

    void requestCreateSessionByManager(long j, RoutingSessionInfo routingSessionInfo, MediaRoute2Info mediaRoute2Info) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IMediaRouter2 {
        @Override // android.media.IMediaRouter2
        public void notifyRouterRegistered(List<MediaRoute2Info> currentRoutes, RoutingSessionInfo currentSystemSessionInfo) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void notifyRoutesAdded(List<MediaRoute2Info> routes) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void notifyRoutesRemoved(List<MediaRoute2Info> routes) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void notifyRoutesChanged(List<MediaRoute2Info> routes) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void notifySessionCreated(int requestId, RoutingSessionInfo sessionInfo) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void notifySessionInfoChanged(RoutingSessionInfo sessionInfo) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void notifySessionReleased(RoutingSessionInfo sessionInfo) throws RemoteException {
        }

        @Override // android.media.IMediaRouter2
        public void requestCreateSessionByManager(long uniqueRequestId, RoutingSessionInfo oldSession, MediaRoute2Info route) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IMediaRouter2 {
        static final int TRANSACTION_notifyRouterRegistered = 1;
        static final int TRANSACTION_notifyRoutesAdded = 2;
        static final int TRANSACTION_notifyRoutesChanged = 4;
        static final int TRANSACTION_notifyRoutesRemoved = 3;
        static final int TRANSACTION_notifySessionCreated = 5;
        static final int TRANSACTION_notifySessionInfoChanged = 6;
        static final int TRANSACTION_notifySessionReleased = 7;
        static final int TRANSACTION_requestCreateSessionByManager = 8;

        public Stub() {
            attachInterface(this, IMediaRouter2.DESCRIPTOR);
        }

        public static IMediaRouter2 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IMediaRouter2.DESCRIPTOR);
            if (iin != null && (iin instanceof IMediaRouter2)) {
                return (IMediaRouter2) iin;
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
                    return "notifyRouterRegistered";
                case 2:
                    return "notifyRoutesAdded";
                case 3:
                    return "notifyRoutesRemoved";
                case 4:
                    return "notifyRoutesChanged";
                case 5:
                    return "notifySessionCreated";
                case 6:
                    return "notifySessionInfoChanged";
                case 7:
                    return "notifySessionReleased";
                case 8:
                    return "requestCreateSessionByManager";
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
                data.enforceInterface(IMediaRouter2.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IMediaRouter2.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            List<MediaRoute2Info> _arg0 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            RoutingSessionInfo _arg1 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRouterRegistered(_arg0, _arg1);
                            break;
                        case 2:
                            List<MediaRoute2Info> _arg02 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRoutesAdded(_arg02);
                            break;
                        case 3:
                            List<MediaRoute2Info> _arg03 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRoutesRemoved(_arg03);
                            break;
                        case 4:
                            List<MediaRoute2Info> _arg04 = data.createTypedArrayList(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            notifyRoutesChanged(_arg04);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            RoutingSessionInfo _arg12 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifySessionCreated(_arg05, _arg12);
                            break;
                        case 6:
                            RoutingSessionInfo _arg06 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifySessionInfoChanged(_arg06);
                            break;
                        case 7:
                            RoutingSessionInfo _arg07 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifySessionReleased(_arg07);
                            break;
                        case 8:
                            long _arg08 = data.readLong();
                            RoutingSessionInfo _arg13 = (RoutingSessionInfo) data.readTypedObject(RoutingSessionInfo.CREATOR);
                            MediaRoute2Info _arg2 = (MediaRoute2Info) data.readTypedObject(MediaRoute2Info.CREATOR);
                            data.enforceNoDataAvail();
                            requestCreateSessionByManager(_arg08, _arg13, _arg2);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IMediaRouter2 {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IMediaRouter2.DESCRIPTOR;
            }

            @Override // android.media.IMediaRouter2
            public void notifyRouterRegistered(List<MediaRoute2Info> currentRoutes, RoutingSessionInfo currentSystemSessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeTypedList(currentRoutes);
                    _data.writeTypedObject(currentSystemSessionInfo, 0);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void notifyRoutesAdded(List<MediaRoute2Info> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void notifyRoutesRemoved(List<MediaRoute2Info> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void notifyRoutesChanged(List<MediaRoute2Info> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeTypedList(routes);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void notifySessionCreated(int requestId, RoutingSessionInfo sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeInt(requestId);
                    _data.writeTypedObject(sessionInfo, 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void notifySessionInfoChanged(RoutingSessionInfo sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeTypedObject(sessionInfo, 0);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void notifySessionReleased(RoutingSessionInfo sessionInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeTypedObject(sessionInfo, 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.IMediaRouter2
            public void requestCreateSessionByManager(long uniqueRequestId, RoutingSessionInfo oldSession, MediaRoute2Info route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IMediaRouter2.DESCRIPTOR);
                    _data.writeLong(uniqueRequestId);
                    _data.writeTypedObject(oldSession, 0);
                    _data.writeTypedObject(route, 0);
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
