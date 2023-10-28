package android.media.tv;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.InputChannel;
import java.util.List;
/* loaded from: classes2.dex */
public interface ITvInputClient extends IInterface {
    void onAdResponse(AdResponse adResponse, int i) throws RemoteException;

    void onAitInfoUpdated(AitInfo aitInfo, int i) throws RemoteException;

    void onBroadcastInfoResponse(BroadcastInfoResponse broadcastInfoResponse, int i) throws RemoteException;

    void onChannelRetuned(Uri uri, int i) throws RemoteException;

    void onContentAllowed(int i) throws RemoteException;

    void onContentBlocked(String str, int i) throws RemoteException;

    void onError(int i, int i2) throws RemoteException;

    void onLayoutSurface(int i, int i2, int i3, int i4, int i5) throws RemoteException;

    void onRecordingStopped(Uri uri, int i) throws RemoteException;

    void onSessionCreated(String str, IBinder iBinder, InputChannel inputChannel, int i) throws RemoteException;

    void onSessionEvent(String str, Bundle bundle, int i) throws RemoteException;

    void onSessionReleased(int i) throws RemoteException;

    void onSignalStrength(int i, int i2) throws RemoteException;

    void onTimeShiftCurrentPositionChanged(long j, int i) throws RemoteException;

    void onTimeShiftStartPositionChanged(long j, int i) throws RemoteException;

    void onTimeShiftStatusChanged(int i, int i2) throws RemoteException;

    void onTrackSelected(int i, String str, int i2) throws RemoteException;

    void onTracksChanged(List<TvTrackInfo> list, int i) throws RemoteException;

    void onTuned(Uri uri, int i) throws RemoteException;

    void onVideoAvailable(int i) throws RemoteException;

    void onVideoUnavailable(int i, int i2) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInputClient {
        @Override // android.media.tv.ITvInputClient
        public void onSessionCreated(String inputId, IBinder token, InputChannel channel, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onSessionReleased(int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onSessionEvent(String name, Bundle args, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onChannelRetuned(Uri channelUri, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onTracksChanged(List<TvTrackInfo> tracks, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onTrackSelected(int type, String trackId, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onVideoAvailable(int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onVideoUnavailable(int reason, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onContentAllowed(int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onContentBlocked(String rating, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onLayoutSurface(int left, int top, int right, int bottom, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onTimeShiftStatusChanged(int status, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onTimeShiftStartPositionChanged(long timeMs, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onTimeShiftCurrentPositionChanged(long timeMs, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onAitInfoUpdated(AitInfo aitInfo, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onSignalStrength(int stength, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onTuned(Uri channelUri, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onRecordingStopped(Uri recordedProgramUri, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onError(int error, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onBroadcastInfoResponse(BroadcastInfoResponse response, int seq) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputClient
        public void onAdResponse(AdResponse response, int seq) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInputClient {
        public static final String DESCRIPTOR = "android.media.tv.ITvInputClient";
        static final int TRANSACTION_onAdResponse = 21;
        static final int TRANSACTION_onAitInfoUpdated = 15;
        static final int TRANSACTION_onBroadcastInfoResponse = 20;
        static final int TRANSACTION_onChannelRetuned = 4;
        static final int TRANSACTION_onContentAllowed = 9;
        static final int TRANSACTION_onContentBlocked = 10;
        static final int TRANSACTION_onError = 19;
        static final int TRANSACTION_onLayoutSurface = 11;
        static final int TRANSACTION_onRecordingStopped = 18;
        static final int TRANSACTION_onSessionCreated = 1;
        static final int TRANSACTION_onSessionEvent = 3;
        static final int TRANSACTION_onSessionReleased = 2;
        static final int TRANSACTION_onSignalStrength = 16;
        static final int TRANSACTION_onTimeShiftCurrentPositionChanged = 14;
        static final int TRANSACTION_onTimeShiftStartPositionChanged = 13;
        static final int TRANSACTION_onTimeShiftStatusChanged = 12;
        static final int TRANSACTION_onTrackSelected = 6;
        static final int TRANSACTION_onTracksChanged = 5;
        static final int TRANSACTION_onTuned = 17;
        static final int TRANSACTION_onVideoAvailable = 7;
        static final int TRANSACTION_onVideoUnavailable = 8;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITvInputClient asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInputClient)) {
                return (ITvInputClient) iin;
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
                    return "onSessionEvent";
                case 4:
                    return "onChannelRetuned";
                case 5:
                    return "onTracksChanged";
                case 6:
                    return "onTrackSelected";
                case 7:
                    return "onVideoAvailable";
                case 8:
                    return "onVideoUnavailable";
                case 9:
                    return "onContentAllowed";
                case 10:
                    return "onContentBlocked";
                case 11:
                    return "onLayoutSurface";
                case 12:
                    return "onTimeShiftStatusChanged";
                case 13:
                    return "onTimeShiftStartPositionChanged";
                case 14:
                    return "onTimeShiftCurrentPositionChanged";
                case 15:
                    return "onAitInfoUpdated";
                case 16:
                    return "onSignalStrength";
                case 17:
                    return "onTuned";
                case 18:
                    return "onRecordingStopped";
                case 19:
                    return "onError";
                case 20:
                    return "onBroadcastInfoResponse";
                case 21:
                    return "onAdResponse";
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
                            String _arg03 = data.readString();
                            Bundle _arg12 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            onSessionEvent(_arg03, _arg12, _arg22);
                            break;
                        case 4:
                            Uri _arg04 = (Uri) data.readTypedObject(Uri.CREATOR);
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            onChannelRetuned(_arg04, _arg13);
                            break;
                        case 5:
                            List<TvTrackInfo> _arg05 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            onTracksChanged(_arg05, _arg14);
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            String _arg15 = data.readString();
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            onTrackSelected(_arg06, _arg15, _arg23);
                            break;
                        case 7:
                            int _arg07 = data.readInt();
                            data.enforceNoDataAvail();
                            onVideoAvailable(_arg07);
                            break;
                        case 8:
                            int _arg08 = data.readInt();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            onVideoUnavailable(_arg08, _arg16);
                            break;
                        case 9:
                            int _arg09 = data.readInt();
                            data.enforceNoDataAvail();
                            onContentAllowed(_arg09);
                            break;
                        case 10:
                            String _arg010 = data.readString();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            onContentBlocked(_arg010, _arg17);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            int _arg18 = data.readInt();
                            int _arg24 = data.readInt();
                            int _arg32 = data.readInt();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            onLayoutSurface(_arg011, _arg18, _arg24, _arg32, _arg4);
                            break;
                        case 12:
                            int _arg012 = data.readInt();
                            int _arg19 = data.readInt();
                            data.enforceNoDataAvail();
                            onTimeShiftStatusChanged(_arg012, _arg19);
                            break;
                        case 13:
                            long _arg013 = data.readLong();
                            int _arg110 = data.readInt();
                            data.enforceNoDataAvail();
                            onTimeShiftStartPositionChanged(_arg013, _arg110);
                            break;
                        case 14:
                            long _arg014 = data.readLong();
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            onTimeShiftCurrentPositionChanged(_arg014, _arg111);
                            break;
                        case 15:
                            AitInfo _arg015 = (AitInfo) data.readTypedObject(AitInfo.CREATOR);
                            int _arg112 = data.readInt();
                            data.enforceNoDataAvail();
                            onAitInfoUpdated(_arg015, _arg112);
                            break;
                        case 16:
                            int _arg016 = data.readInt();
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            onSignalStrength(_arg016, _arg113);
                            break;
                        case 17:
                            Uri _arg017 = (Uri) data.readTypedObject(Uri.CREATOR);
                            int _arg114 = data.readInt();
                            data.enforceNoDataAvail();
                            onTuned(_arg017, _arg114);
                            break;
                        case 18:
                            Uri _arg018 = (Uri) data.readTypedObject(Uri.CREATOR);
                            int _arg115 = data.readInt();
                            data.enforceNoDataAvail();
                            onRecordingStopped(_arg018, _arg115);
                            break;
                        case 19:
                            int _arg019 = data.readInt();
                            int _arg116 = data.readInt();
                            data.enforceNoDataAvail();
                            onError(_arg019, _arg116);
                            break;
                        case 20:
                            BroadcastInfoResponse _arg020 = (BroadcastInfoResponse) data.readTypedObject(BroadcastInfoResponse.CREATOR);
                            int _arg117 = data.readInt();
                            data.enforceNoDataAvail();
                            onBroadcastInfoResponse(_arg020, _arg117);
                            break;
                        case 21:
                            AdResponse _arg021 = (AdResponse) data.readTypedObject(AdResponse.CREATOR);
                            int _arg118 = data.readInt();
                            data.enforceNoDataAvail();
                            onAdResponse(_arg021, _arg118);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITvInputClient {
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

            @Override // android.media.tv.ITvInputClient
            public void onSessionCreated(String inputId, IBinder token, InputChannel channel, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(inputId);
                    _data.writeStrongBinder(token);
                    _data.writeTypedObject(channel, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onSessionReleased(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onSessionEvent(String name, Bundle args, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeTypedObject(args, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onChannelRetuned(Uri channelUri, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onTracksChanged(List<TvTrackInfo> tracks, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(tracks);
                    _data.writeInt(seq);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onTrackSelected(int type, String trackId, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(trackId);
                    _data.writeInt(seq);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onVideoAvailable(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onVideoUnavailable(int reason, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    _data.writeInt(seq);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onContentAllowed(int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onContentBlocked(String rating, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(rating);
                    _data.writeInt(seq);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onLayoutSurface(int left, int top, int right, int bottom, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(right);
                    _data.writeInt(bottom);
                    _data.writeInt(seq);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onTimeShiftStatusChanged(int status, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeInt(seq);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onTimeShiftStartPositionChanged(long timeMs, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timeMs);
                    _data.writeInt(seq);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onTimeShiftCurrentPositionChanged(long timeMs, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timeMs);
                    _data.writeInt(seq);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onAitInfoUpdated(AitInfo aitInfo, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(aitInfo, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onSignalStrength(int stength, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(stength);
                    _data.writeInt(seq);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onTuned(Uri channelUri, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onRecordingStopped(Uri recordedProgramUri, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(recordedProgramUri, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onError(int error, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(error);
                    _data.writeInt(seq);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onBroadcastInfoResponse(BroadcastInfoResponse response, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputClient
            public void onAdResponse(AdResponse response, int seq) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    _data.writeInt(seq);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 20;
        }
    }
}
