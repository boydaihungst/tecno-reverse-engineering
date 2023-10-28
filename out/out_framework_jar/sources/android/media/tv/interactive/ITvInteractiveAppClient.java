package android.media.tv.interactive;

import android.graphics.Rect;
import android.media.tv.AdRequest;
import android.media.tv.BroadcastInfoRequest;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.InputChannel;
/* loaded from: classes2.dex */
public interface ITvInteractiveAppClient extends IInterface {
    public static final String DESCRIPTOR = "android.media.tv.interactive.ITvInteractiveAppClient";

    void onAdRequest(AdRequest adRequest, int i) throws RemoteException;

    void onBiInteractiveAppCreated(Uri uri, String str, int i) throws RemoteException;

    void onBroadcastInfoRequest(BroadcastInfoRequest broadcastInfoRequest, int i) throws RemoteException;

    void onCommandRequest(String str, Bundle bundle, int i) throws RemoteException;

    void onLayoutSurface(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void onRemoveBroadcastInfo(int i, int i2) throws RemoteException;

    void onRequestCurrentChannelLcn(int i) throws RemoteException;

    void onRequestCurrentChannelUri(int i) throws RemoteException;

    void onRequestCurrentTvInputId(int i) throws RemoteException;

    void onRequestSigning(String str, String str2, String str3, byte[] bArr, int i) throws RemoteException;

    void onRequestStreamVolume(int i) throws RemoteException;

    void onRequestTrackInfoList(int i) throws RemoteException;

    void onSessionCreated(String str, IBinder iBinder, InputChannel inputChannel, int i) throws RemoteException;

    void onSessionReleased(int i) throws RemoteException;

    void onSessionStateChanged(int i, int i2, int i3) throws RemoteException;

    void onSetVideoBounds(Rect rect, int i) throws RemoteException;

    void onTeletextAppStateChanged(int i, int i2) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInteractiveAppClient {
        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onSessionCreated(String iAppServiceId, IBinder token, InputChannel channel, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onSessionReleased(int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onLayoutSurface(int left, int top, int right, int bottom, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onBroadcastInfoRequest(BroadcastInfoRequest request, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRemoveBroadcastInfo(int id, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onSessionStateChanged(int state, int err, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onBiInteractiveAppCreated(Uri biIAppUri, String biIAppId, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onTeletextAppStateChanged(int state, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onCommandRequest(String cmdType, Bundle parameters, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onSetVideoBounds(Rect rect, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRequestCurrentChannelUri(int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRequestCurrentChannelLcn(int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRequestStreamVolume(int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRequestTrackInfoList(int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRequestCurrentTvInputId(int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onRequestSigning(String id, String algorithm, String alias, byte[] data, int seq) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppClient
        public void onAdRequest(AdRequest request, int Seq) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInteractiveAppClient {
        static final int TRANSACTION_onAdRequest = 17;
        static final int TRANSACTION_onBiInteractiveAppCreated = 7;
        static final int TRANSACTION_onBroadcastInfoRequest = 4;
        static final int TRANSACTION_onCommandRequest = 9;
        static final int TRANSACTION_onLayoutSurface = 3;
        static final int TRANSACTION_onRemoveBroadcastInfo = 5;
        static final int TRANSACTION_onRequestCurrentChannelLcn = 12;
        static final int TRANSACTION_onRequestCurrentChannelUri = 11;
        static final int TRANSACTION_onRequestCurrentTvInputId = 15;
        static final int TRANSACTION_onRequestSigning = 16;
        static final int TRANSACTION_onRequestStreamVolume = 13;
        static final int TRANSACTION_onRequestTrackInfoList = 14;
        static final int TRANSACTION_onSessionCreated = 1;
        static final int TRANSACTION_onSessionReleased = 2;
        static final int TRANSACTION_onSessionStateChanged = 6;
        static final int TRANSACTION_onSetVideoBounds = 10;
        static final int TRANSACTION_onTeletextAppStateChanged = 8;

        public Stub() {
            attachInterface(this, ITvInteractiveAppClient.DESCRIPTOR);
        }

        public static ITvInteractiveAppClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITvInteractiveAppClient.DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInteractiveAppClient)) {
                return (ITvInteractiveAppClient) iin;
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
                    return "onSessionReleased";
                case 3:
                    return "onLayoutSurface";
                case 4:
                    return "onBroadcastInfoRequest";
                case 5:
                    return "onRemoveBroadcastInfo";
                case 6:
                    return "onSessionStateChanged";
                case 7:
                    return "onBiInteractiveAppCreated";
                case 8:
                    return "onTeletextAppStateChanged";
                case 9:
                    return "onCommandRequest";
                case 10:
                    return "onSetVideoBounds";
                case 11:
                    return "onRequestCurrentChannelUri";
                case 12:
                    return "onRequestCurrentChannelLcn";
                case 13:
                    return "onRequestStreamVolume";
                case 14:
                    return "onRequestTrackInfoList";
                case 15:
                    return "onRequestCurrentTvInputId";
                case 16:
                    return "onRequestSigning";
                case 17:
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
                data.enforceInterface(ITvInteractiveAppClient.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITvInteractiveAppClient.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            IBinder _arg1 = data.readStrongBinder();
                            InputChannel _arg2 = (InputChannel) data.readTypedObject(InputChannel.CREATOR);
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            onSessionCreated(_arg0, _arg1, _arg2, _arg3);
                            break;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            onSessionReleased(_arg02);
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            int _arg12 = data.readInt();
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            onLayoutSurface(_arg03, _arg12, _arg22, _arg32, _arg4);
                            break;
                        case 4:
                            BroadcastInfoRequest _arg04 = (BroadcastInfoRequest) data.readTypedObject(BroadcastInfoRequest.CREATOR);
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            onBroadcastInfoRequest(_arg04, _arg13);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            onRemoveBroadcastInfo(_arg05, _arg14);
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            int _arg15 = data.readInt();
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            onSessionStateChanged(_arg06, _arg15, _arg23);
                            break;
                        case 7:
                            Uri _arg07 = (Uri) data.readTypedObject(Uri.CREATOR);
                            String _arg16 = data.readString();
                            int _arg24 = data.readInt();
                            data.enforceNoDataAvail();
                            onBiInteractiveAppCreated(_arg07, _arg16, _arg24);
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            onTeletextAppStateChanged(_arg08, _arg17);
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            Bundle _arg18 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            onCommandRequest(_arg09, _arg18, _arg25);
                            break;
                        case 10:
                            Rect _arg010 = (Rect) data.readTypedObject(Rect.CREATOR);
                            int _arg19 = data.readInt();
                            data.enforceNoDataAvail();
                            onSetVideoBounds(_arg010, _arg19);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            data.enforceNoDataAvail();
                            onRequestCurrentChannelUri(_arg011);
                            break;
                        case 12:
                            int _arg012 = data.readInt();
                            data.enforceNoDataAvail();
                            onRequestCurrentChannelLcn(_arg012);
                            break;
                        case 13:
                            int _arg013 = data.readInt();
                            data.enforceNoDataAvail();
                            onRequestStreamVolume(_arg013);
                            break;
                        case 14:
                            int _arg014 = data.readInt();
                            data.enforceNoDataAvail();
                            onRequestTrackInfoList(_arg014);
                            break;
                        case 15:
                            int _arg015 = data.readInt();
                            data.enforceNoDataAvail();
                            onRequestCurrentTvInputId(_arg015);
                            break;
                        case 16:
                            String _arg016 = data.readString();
                            String _arg110 = data.readString();
                            String _arg26 = data.readString();
                            byte[] _arg33 = data.createByteArray();
                            int _arg42 = data.readInt();
                            data.enforceNoDataAvail();
                            onRequestSigning(_arg016, _arg110, _arg26, _arg33, _arg42);
                            break;
                        case 17:
                            AdRequest _arg017 = (AdRequest) data.readTypedObject(AdRequest.CREATOR);
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            onAdRequest(_arg017, _arg111);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITvInteractiveAppClient {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITvInteractiveAppClient.DESCRIPTOR;
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onSessionCreated(String iAppServiceId, IBinder token, InputChannel channel, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeString(iAppServiceId);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(channel, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onSessionReleased(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onLayoutSurface(int left, int top, int right, int bottom, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(right);
                    _data.writeInt(bottom);
                    _data.writeInt(seq);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onBroadcastInfoRequest(BroadcastInfoRequest request, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRemoveBroadcastInfo(int id, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeInt(seq);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onSessionStateChanged(int state, int err, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(err);
                    _data.writeInt(seq);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onBiInteractiveAppCreated(Uri biIAppUri, String biIAppId, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeTypedObject(biIAppUri, 0);
                    _data.writeString(biIAppId);
                    _data.writeInt(seq);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onTeletextAppStateChanged(int state, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(seq);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onCommandRequest(String cmdType, Bundle parameters, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeString(cmdType);
                    _data.writeTypedObject(parameters, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onSetVideoBounds(Rect rect, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeTypedObject(rect, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRequestCurrentChannelUri(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRequestCurrentChannelLcn(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRequestStreamVolume(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRequestTrackInfoList(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRequestCurrentTvInputId(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onRequestSigning(String id, String algorithm, String alias, byte[] data, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeString(id);
                    _data.writeString(algorithm);
                    _data.writeString(alias);
                    _data.writeByteArray(data);
                    _data.writeInt(seq);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppClient
            public void onAdRequest(AdRequest request, int Seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppClient.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    _data.writeInt(Seq);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
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
