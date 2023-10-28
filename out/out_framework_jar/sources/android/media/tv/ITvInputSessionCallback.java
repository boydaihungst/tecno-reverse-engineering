package android.media.tv;

import android.media.tv.ITvInputSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes2.dex */
public interface ITvInputSessionCallback extends IInterface {
    void onAdResponse(AdResponse adResponse) throws RemoteException;

    void onAitInfoUpdated(AitInfo aitInfo) throws RemoteException;

    void onBroadcastInfoResponse(BroadcastInfoResponse broadcastInfoResponse) throws RemoteException;

    void onChannelRetuned(Uri uri) throws RemoteException;

    void onContentAllowed() throws RemoteException;

    void onContentBlocked(String str) throws RemoteException;

    void onError(int i) throws RemoteException;

    void onLayoutSurface(int i, int i2, int i3, int i4) throws RemoteException;

    void onRecordingStopped(Uri uri) throws RemoteException;

    void onSessionCreated(ITvInputSession iTvInputSession, IBinder iBinder) throws RemoteException;

    void onSessionEvent(String str, Bundle bundle) throws RemoteException;

    void onSignalStrength(int i) throws RemoteException;

    void onTimeShiftCurrentPositionChanged(long j) throws RemoteException;

    void onTimeShiftStartPositionChanged(long j) throws RemoteException;

    void onTimeShiftStatusChanged(int i) throws RemoteException;

    void onTrackSelected(int i, String str) throws RemoteException;

    void onTracksChanged(List<TvTrackInfo> list) throws RemoteException;

    void onTuned(Uri uri) throws RemoteException;

    void onVideoAvailable() throws RemoteException;

    void onVideoUnavailable(int i) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInputSessionCallback {
        @Override // android.media.tv.ITvInputSessionCallback
        public void onSessionCreated(ITvInputSession session, IBinder hardwareSessionToken) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onSessionEvent(String name, Bundle args) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onChannelRetuned(Uri channelUri) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onTracksChanged(List<TvTrackInfo> tracks) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onTrackSelected(int type, String trackId) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onVideoAvailable() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onVideoUnavailable(int reason) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onContentAllowed() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onContentBlocked(String rating) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onLayoutSurface(int left, int top, int right, int bottom) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onTimeShiftStatusChanged(int status) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onTimeShiftStartPositionChanged(long timeMs) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onTimeShiftCurrentPositionChanged(long timeMs) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onAitInfoUpdated(AitInfo aitInfo) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onSignalStrength(int strength) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onTuned(Uri channelUri) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onRecordingStopped(Uri recordedProgramUri) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onError(int error) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onBroadcastInfoResponse(BroadcastInfoResponse response) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSessionCallback
        public void onAdResponse(AdResponse response) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInputSessionCallback {
        public static final String DESCRIPTOR = "android.media.tv.ITvInputSessionCallback";
        static final int TRANSACTION_onAdResponse = 20;
        static final int TRANSACTION_onAitInfoUpdated = 14;
        static final int TRANSACTION_onBroadcastInfoResponse = 19;
        static final int TRANSACTION_onChannelRetuned = 3;
        static final int TRANSACTION_onContentAllowed = 8;
        static final int TRANSACTION_onContentBlocked = 9;
        static final int TRANSACTION_onError = 18;
        static final int TRANSACTION_onLayoutSurface = 10;
        static final int TRANSACTION_onRecordingStopped = 17;
        static final int TRANSACTION_onSessionCreated = 1;
        static final int TRANSACTION_onSessionEvent = 2;
        static final int TRANSACTION_onSignalStrength = 15;
        static final int TRANSACTION_onTimeShiftCurrentPositionChanged = 13;
        static final int TRANSACTION_onTimeShiftStartPositionChanged = 12;
        static final int TRANSACTION_onTimeShiftStatusChanged = 11;
        static final int TRANSACTION_onTrackSelected = 5;
        static final int TRANSACTION_onTracksChanged = 4;
        static final int TRANSACTION_onTuned = 16;
        static final int TRANSACTION_onVideoAvailable = 6;
        static final int TRANSACTION_onVideoUnavailable = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITvInputSessionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInputSessionCallback)) {
                return (ITvInputSessionCallback) iin;
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
                    return "onSessionEvent";
                case 3:
                    return "onChannelRetuned";
                case 4:
                    return "onTracksChanged";
                case 5:
                    return "onTrackSelected";
                case 6:
                    return "onVideoAvailable";
                case 7:
                    return "onVideoUnavailable";
                case 8:
                    return "onContentAllowed";
                case 9:
                    return "onContentBlocked";
                case 10:
                    return "onLayoutSurface";
                case 11:
                    return "onTimeShiftStatusChanged";
                case 12:
                    return "onTimeShiftStartPositionChanged";
                case 13:
                    return "onTimeShiftCurrentPositionChanged";
                case 14:
                    return "onAitInfoUpdated";
                case 15:
                    return "onSignalStrength";
                case 16:
                    return "onTuned";
                case 17:
                    return "onRecordingStopped";
                case 18:
                    return "onError";
                case 19:
                    return "onBroadcastInfoResponse";
                case 20:
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
                            ITvInputSession _arg0 = ITvInputSession.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg1 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            onSessionCreated(_arg0, _arg1);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            Bundle _arg12 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onSessionEvent(_arg02, _arg12);
                            break;
                        case 3:
                            Uri _arg03 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            onChannelRetuned(_arg03);
                            break;
                        case 4:
                            List<TvTrackInfo> _arg04 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onTracksChanged(_arg04);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            onTrackSelected(_arg05, _arg13);
                            break;
                        case 6:
                            onVideoAvailable();
                            break;
                        case 7:
                            int _arg06 = data.readInt();
                            data.enforceNoDataAvail();
                            onVideoUnavailable(_arg06);
                            break;
                        case 8:
                            onContentAllowed();
                            break;
                        case 9:
                            String _arg07 = data.readString();
                            data.enforceNoDataAvail();
                            onContentBlocked(_arg07);
                            break;
                        case 10:
                            int _arg08 = data.readInt();
                            int _arg14 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            onLayoutSurface(_arg08, _arg14, _arg2, _arg3);
                            break;
                        case 11:
                            int _arg09 = data.readInt();
                            data.enforceNoDataAvail();
                            onTimeShiftStatusChanged(_arg09);
                            break;
                        case 12:
                            long _arg010 = data.readLong();
                            data.enforceNoDataAvail();
                            onTimeShiftStartPositionChanged(_arg010);
                            break;
                        case 13:
                            long _arg011 = data.readLong();
                            data.enforceNoDataAvail();
                            onTimeShiftCurrentPositionChanged(_arg011);
                            break;
                        case 14:
                            AitInfo _arg012 = (AitInfo) data.readTypedObject(AitInfo.CREATOR);
                            data.enforceNoDataAvail();
                            onAitInfoUpdated(_arg012);
                            break;
                        case 15:
                            int _arg013 = data.readInt();
                            data.enforceNoDataAvail();
                            onSignalStrength(_arg013);
                            break;
                        case 16:
                            Uri _arg014 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            onTuned(_arg014);
                            break;
                        case 17:
                            Uri _arg015 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            onRecordingStopped(_arg015);
                            break;
                        case 18:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            onError(_arg016);
                            break;
                        case 19:
                            BroadcastInfoResponse _arg017 = (BroadcastInfoResponse) data.readTypedObject(BroadcastInfoResponse.CREATOR);
                            data.enforceNoDataAvail();
                            onBroadcastInfoResponse(_arg017);
                            break;
                        case 20:
                            AdResponse _arg018 = (AdResponse) data.readTypedObject(AdResponse.CREATOR);
                            data.enforceNoDataAvail();
                            onAdResponse(_arg018);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITvInputSessionCallback {
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

            @Override // android.media.tv.ITvInputSessionCallback
            public void onSessionCreated(ITvInputSession session, IBinder hardwareSessionToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(session);
                    _data.writeStrongBinder(hardwareSessionToken);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onSessionEvent(String name, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeTypedObject(args, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onChannelRetuned(Uri channelUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onTracksChanged(List<TvTrackInfo> tracks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(tracks);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onTrackSelected(int type, String trackId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(trackId);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onVideoAvailable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onVideoUnavailable(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onContentAllowed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onContentBlocked(String rating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(rating);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onLayoutSurface(int left, int top, int right, int bottom) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(left);
                    _data.writeInt(top);
                    _data.writeInt(right);
                    _data.writeInt(bottom);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onTimeShiftStatusChanged(int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onTimeShiftStartPositionChanged(long timeMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timeMs);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onTimeShiftCurrentPositionChanged(long timeMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timeMs);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onAitInfoUpdated(AitInfo aitInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(aitInfo, 0);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onSignalStrength(int strength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(strength);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onTuned(Uri channelUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onRecordingStopped(Uri recordedProgramUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(recordedProgramUri, 0);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onError(int error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(error);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onBroadcastInfoResponse(BroadcastInfoResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSessionCallback
            public void onAdResponse(AdResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 19;
        }
    }
}
