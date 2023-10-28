package android.media.tv.interactive;

import android.graphics.Rect;
import android.media.tv.AdRequest;
import android.media.tv.BroadcastInfoRequest;
import android.media.tv.interactive.ITvInteractiveAppSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes2.dex */
public interface ITvInteractiveAppSessionCallback extends IInterface {
    public static final String DESCRIPTOR = "android.media.tv.interactive.ITvInteractiveAppSessionCallback";

    void onAdRequest(AdRequest adRequest) throws RemoteException;

    void onBiInteractiveAppCreated(Uri uri, String str) throws RemoteException;

    void onBroadcastInfoRequest(BroadcastInfoRequest broadcastInfoRequest) throws RemoteException;

    void onCommandRequest(String str, Bundle bundle) throws RemoteException;

    void onLayoutSurface(int i, int i2, int i3, int i4) throws RemoteException;

    void onRemoveBroadcastInfo(int i) throws RemoteException;

    void onRequestCurrentChannelLcn() throws RemoteException;

    void onRequestCurrentChannelUri() throws RemoteException;

    void onRequestCurrentTvInputId() throws RemoteException;

    void onRequestSigning(String str, String str2, String str3, byte[] bArr) throws RemoteException;

    void onRequestStreamVolume() throws RemoteException;

    void onRequestTrackInfoList() throws RemoteException;

    void onSessionCreated(ITvInteractiveAppSession iTvInteractiveAppSession) throws RemoteException;

    void onSessionStateChanged(int i, int i2) throws RemoteException;

    void onSetVideoBounds(Rect rect) throws RemoteException;

    void onTeletextAppStateChanged(int i) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInteractiveAppSessionCallback {
        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onSessionCreated(ITvInteractiveAppSession session) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onLayoutSurface(int left, int top, int right, int bottom) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onBroadcastInfoRequest(BroadcastInfoRequest request) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRemoveBroadcastInfo(int id) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onSessionStateChanged(int state, int err) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onBiInteractiveAppCreated(Uri biIAppUri, String biIAppId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onTeletextAppStateChanged(int state) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onCommandRequest(String cmdType, Bundle parameters) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onSetVideoBounds(Rect rect) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRequestCurrentChannelUri() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRequestCurrentChannelLcn() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRequestStreamVolume() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRequestTrackInfoList() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRequestCurrentTvInputId() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onRequestSigning(String id, String algorithm, String alias, byte[] data) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
        public void onAdRequest(AdRequest request) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInteractiveAppSessionCallback {
        static final int TRANSACTION_onAdRequest = 16;
        static final int TRANSACTION_onBiInteractiveAppCreated = 6;
        static final int TRANSACTION_onBroadcastInfoRequest = 3;
        static final int TRANSACTION_onCommandRequest = 8;
        static final int TRANSACTION_onLayoutSurface = 2;
        static final int TRANSACTION_onRemoveBroadcastInfo = 4;
        static final int TRANSACTION_onRequestCurrentChannelLcn = 11;
        static final int TRANSACTION_onRequestCurrentChannelUri = 10;
        static final int TRANSACTION_onRequestCurrentTvInputId = 14;
        static final int TRANSACTION_onRequestSigning = 15;
        static final int TRANSACTION_onRequestStreamVolume = 12;
        static final int TRANSACTION_onRequestTrackInfoList = 13;
        static final int TRANSACTION_onSessionCreated = 1;
        static final int TRANSACTION_onSessionStateChanged = 5;
        static final int TRANSACTION_onSetVideoBounds = 9;
        static final int TRANSACTION_onTeletextAppStateChanged = 7;

        public Stub() {
            attachInterface(this, ITvInteractiveAppSessionCallback.DESCRIPTOR);
        }

        public static ITvInteractiveAppSessionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITvInteractiveAppSessionCallback.DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInteractiveAppSessionCallback)) {
                return (ITvInteractiveAppSessionCallback) iin;
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
                    return "onSessionCreated";
                case 2:
                    return "onLayoutSurface";
                case 3:
                    return "onBroadcastInfoRequest";
                case 4:
                    return "onRemoveBroadcastInfo";
                case 5:
                    return "onSessionStateChanged";
                case 6:
                    return "onBiInteractiveAppCreated";
                case 7:
                    return "onTeletextAppStateChanged";
                case 8:
                    return "onCommandRequest";
                case 9:
                    return "onSetVideoBounds";
                case 10:
                    return "onRequestCurrentChannelUri";
                case 11:
                    return "onRequestCurrentChannelLcn";
                case 12:
                    return "onRequestStreamVolume";
                case 13:
                    return "onRequestTrackInfoList";
                case 14:
                    return "onRequestCurrentTvInputId";
                case 15:
                    return "onRequestSigning";
                case 16:
                    return "onAdRequest";
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
                data.enforceInterface(ITvInteractiveAppSessionCallback.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            ITvInteractiveAppSession _arg0 = ITvInteractiveAppSession.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onSessionCreated(_arg0);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            onLayoutSurface(_arg02, _arg1, _arg2, _arg3);
                            break;
                        case 3:
                            BroadcastInfoRequest _arg03 = (BroadcastInfoRequest) data.readTypedObject(BroadcastInfoRequest.CREATOR);
                            data.enforceNoDataAvail();
                            onBroadcastInfoRequest(_arg03);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            onRemoveBroadcastInfo(_arg04);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            onSessionStateChanged(_arg05, _arg12);
                            break;
                        case 6:
                            Uri _arg06 = (Uri) data.readTypedObject(Uri.CREATOR);
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            onBiInteractiveAppCreated(_arg06, _arg13);
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            data.enforceNoDataAvail();
                            onTeletextAppStateChanged(_arg07);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            Bundle _arg14 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onCommandRequest(_arg08, _arg14);
                            break;
                        case 9:
                            Rect _arg09 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            onSetVideoBounds(_arg09);
                            break;
                        case 10:
                            onRequestCurrentChannelUri();
                            break;
                        case 11:
                            onRequestCurrentChannelLcn();
                            break;
                        case 12:
                            onRequestStreamVolume();
                            break;
                        case 13:
                            onRequestTrackInfoList();
                            break;
                        case 14:
                            onRequestCurrentTvInputId();
                            break;
                        case 15:
                            String _arg010 = data.readString();
                            String _arg15 = data.readString();
                            String _arg22 = data.readString();
                            byte[] _arg32 = data.createByteArray();
                            data.enforceNoDataAvail();
                            onRequestSigning(_arg010, _arg15, _arg22, _arg32);
                            break;
                        case 16:
                            AdRequest _arg011 = (AdRequest) data.readTypedObject(AdRequest.CREATOR);
                            data.enforceNoDataAvail();
                            onAdRequest(_arg011);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITvInteractiveAppSessionCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITvInteractiveAppSessionCallback.DESCRIPTOR;
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onSessionCreated(ITvInteractiveAppSession session) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeStrongInterface(session);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onLayoutSurface(int left, int top, int right, int bottom) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(right);
                    _data.writeInt(bottom);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onBroadcastInfoRequest(BroadcastInfoRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRemoveBroadcastInfo(int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeInt(id);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onSessionStateChanged(int state, int err) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(err);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onBiInteractiveAppCreated(Uri biIAppUri, String biIAppId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeTypedObject(biIAppUri, 0);
                    _data.writeString(biIAppId);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onTeletextAppStateChanged(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onCommandRequest(String cmdType, Bundle parameters) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeString(cmdType);
                    _data.writeTypedObject(parameters, 0);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onSetVideoBounds(Rect rect) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeTypedObject(rect, 0);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRequestCurrentChannelUri() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRequestCurrentChannelLcn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRequestStreamVolume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRequestTrackInfoList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRequestCurrentTvInputId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onRequestSigning(String id, String algorithm, String alias, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeString(id);
                    _data.writeString(algorithm);
                    _data.writeString(alias);
                    _data.writeByteArray(data);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSessionCallback
            public void onAdRequest(AdRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSessionCallback.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 15;
        }
    }
}
