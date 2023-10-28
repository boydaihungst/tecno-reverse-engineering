package android.media.tv.interactive;

import android.graphics.Rect;
import android.media.tv.AdResponse;
import android.media.tv.BroadcastInfoResponse;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import java.util.List;
/* loaded from: classes2.dex */
public interface ITvInteractiveAppSession extends IInterface {
    public static final String DESCRIPTOR = "android.media.tv.interactive.ITvInteractiveAppSession";

    void createBiInteractiveApp(Uri uri, Bundle bundle) throws RemoteException;

    void createMediaView(IBinder iBinder, Rect rect) throws RemoteException;

    void destroyBiInteractiveApp(String str) throws RemoteException;

    void dispatchSurfaceChanged(int i, int i2, int i3) throws RemoteException;

    void notifyAdResponse(AdResponse adResponse) throws RemoteException;

    void notifyBroadcastInfoResponse(BroadcastInfoResponse broadcastInfoResponse) throws RemoteException;

    void notifyContentAllowed() throws RemoteException;

    void notifyContentBlocked(String str) throws RemoteException;

    void notifyError(String str, Bundle bundle) throws RemoteException;

    void notifySignalStrength(int i) throws RemoteException;

    void notifyTrackSelected(int i, String str) throws RemoteException;

    void notifyTracksChanged(List<TvTrackInfo> list) throws RemoteException;

    void notifyTuned(Uri uri) throws RemoteException;

    void notifyVideoAvailable() throws RemoteException;

    void notifyVideoUnavailable(int i) throws RemoteException;

    void relayoutMediaView(Rect rect) throws RemoteException;

    void release() throws RemoteException;

    void removeMediaView() throws RemoteException;

    void resetInteractiveApp() throws RemoteException;

    void sendCurrentChannelLcn(int i) throws RemoteException;

    void sendCurrentChannelUri(Uri uri) throws RemoteException;

    void sendCurrentTvInputId(String str) throws RemoteException;

    void sendSigningResult(String str, byte[] bArr) throws RemoteException;

    void sendStreamVolume(float f) throws RemoteException;

    void sendTrackInfoList(List<TvTrackInfo> list) throws RemoteException;

    void setSurface(Surface surface) throws RemoteException;

    void setTeletextAppEnabled(boolean z) throws RemoteException;

    void startInteractiveApp() throws RemoteException;

    void stopInteractiveApp() throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInteractiveAppSession {
        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void startInteractiveApp() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void stopInteractiveApp() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void resetInteractiveApp() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void createBiInteractiveApp(Uri biIAppUri, Bundle params) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void destroyBiInteractiveApp(String biIAppId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void setTeletextAppEnabled(boolean enable) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendCurrentChannelUri(Uri channelUri) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendCurrentChannelLcn(int lcn) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendStreamVolume(float volume) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendTrackInfoList(List<TvTrackInfo> tracks) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendCurrentTvInputId(String inputId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void sendSigningResult(String signingId, byte[] result) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyError(String errMsg, Bundle params) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void release() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyTuned(Uri channelUri) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyTrackSelected(int type, String trackId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyTracksChanged(List<TvTrackInfo> tracks) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyVideoAvailable() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyVideoUnavailable(int reason) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyContentAllowed() throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyContentBlocked(String rating) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifySignalStrength(int strength) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void setSurface(Surface surface) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void dispatchSurfaceChanged(int format, int width, int height) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyBroadcastInfoResponse(BroadcastInfoResponse response) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void notifyAdResponse(AdResponse response) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void createMediaView(IBinder windowToken, Rect frame) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void relayoutMediaView(Rect frame) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppSession
        public void removeMediaView() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInteractiveAppSession {
        static final int TRANSACTION_createBiInteractiveApp = 4;
        static final int TRANSACTION_createMediaView = 27;
        static final int TRANSACTION_destroyBiInteractiveApp = 5;
        static final int TRANSACTION_dispatchSurfaceChanged = 24;
        static final int TRANSACTION_notifyAdResponse = 26;
        static final int TRANSACTION_notifyBroadcastInfoResponse = 25;
        static final int TRANSACTION_notifyContentAllowed = 20;
        static final int TRANSACTION_notifyContentBlocked = 21;
        static final int TRANSACTION_notifyError = 13;
        static final int TRANSACTION_notifySignalStrength = 22;
        static final int TRANSACTION_notifyTrackSelected = 16;
        static final int TRANSACTION_notifyTracksChanged = 17;
        static final int TRANSACTION_notifyTuned = 15;
        static final int TRANSACTION_notifyVideoAvailable = 18;
        static final int TRANSACTION_notifyVideoUnavailable = 19;
        static final int TRANSACTION_relayoutMediaView = 28;
        static final int TRANSACTION_release = 14;
        static final int TRANSACTION_removeMediaView = 29;
        static final int TRANSACTION_resetInteractiveApp = 3;
        static final int TRANSACTION_sendCurrentChannelLcn = 8;
        static final int TRANSACTION_sendCurrentChannelUri = 7;
        static final int TRANSACTION_sendCurrentTvInputId = 11;
        static final int TRANSACTION_sendSigningResult = 12;
        static final int TRANSACTION_sendStreamVolume = 9;
        static final int TRANSACTION_sendTrackInfoList = 10;
        static final int TRANSACTION_setSurface = 23;
        static final int TRANSACTION_setTeletextAppEnabled = 6;
        static final int TRANSACTION_startInteractiveApp = 1;
        static final int TRANSACTION_stopInteractiveApp = 2;

        public Stub() {
            attachInterface(this, ITvInteractiveAppSession.DESCRIPTOR);
        }

        public static ITvInteractiveAppSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITvInteractiveAppSession.DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInteractiveAppSession)) {
                return (ITvInteractiveAppSession) iin;
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
                    return "startInteractiveApp";
                case 2:
                    return "stopInteractiveApp";
                case 3:
                    return "resetInteractiveApp";
                case 4:
                    return "createBiInteractiveApp";
                case 5:
                    return "destroyBiInteractiveApp";
                case 6:
                    return "setTeletextAppEnabled";
                case 7:
                    return "sendCurrentChannelUri";
                case 8:
                    return "sendCurrentChannelLcn";
                case 9:
                    return "sendStreamVolume";
                case 10:
                    return "sendTrackInfoList";
                case 11:
                    return "sendCurrentTvInputId";
                case 12:
                    return "sendSigningResult";
                case 13:
                    return "notifyError";
                case 14:
                    return "release";
                case 15:
                    return "notifyTuned";
                case 16:
                    return "notifyTrackSelected";
                case 17:
                    return "notifyTracksChanged";
                case 18:
                    return "notifyVideoAvailable";
                case 19:
                    return "notifyVideoUnavailable";
                case 20:
                    return "notifyContentAllowed";
                case 21:
                    return "notifyContentBlocked";
                case 22:
                    return "notifySignalStrength";
                case 23:
                    return "setSurface";
                case 24:
                    return "dispatchSurfaceChanged";
                case 25:
                    return "notifyBroadcastInfoResponse";
                case 26:
                    return "notifyAdResponse";
                case 27:
                    return "createMediaView";
                case 28:
                    return "relayoutMediaView";
                case 29:
                    return "removeMediaView";
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
                data.enforceInterface(ITvInteractiveAppSession.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITvInteractiveAppSession.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            startInteractiveApp();
                            break;
                        case 2:
                            stopInteractiveApp();
                            break;
                        case 3:
                            resetInteractiveApp();
                            break;
                        case 4:
                            Uri _arg0 = (Uri) data.readTypedObject(Uri.CREATOR);
                            Bundle _arg1 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            createBiInteractiveApp(_arg0, _arg1);
                            break;
                        case 5:
                            String _arg02 = data.readString();
                            data.enforceNoDataAvail();
                            destroyBiInteractiveApp(_arg02);
                            break;
                        case 6:
                            boolean _arg03 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setTeletextAppEnabled(_arg03);
                            break;
                        case 7:
                            Uri _arg04 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            sendCurrentChannelUri(_arg04);
                            break;
                        case 8:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            sendCurrentChannelLcn(_arg05);
                            break;
                        case 9:
                            float _arg06 = data.readFloat();
                            data.enforceNoDataAvail();
                            sendStreamVolume(_arg06);
                            break;
                        case 10:
                            List<TvTrackInfo> _arg07 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                            data.enforceNoDataAvail();
                            sendTrackInfoList(_arg07);
                            break;
                        case 11:
                            String _arg08 = data.readString();
                            data.enforceNoDataAvail();
                            sendCurrentTvInputId(_arg08);
                            break;
                        case 12:
                            String _arg09 = data.readString();
                            byte[] _arg12 = data.createByteArray();
                            data.enforceNoDataAvail();
                            sendSigningResult(_arg09, _arg12);
                            break;
                        case 13:
                            String _arg010 = data.readString();
                            Bundle _arg13 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            notifyError(_arg010, _arg13);
                            break;
                        case 14:
                            release();
                            break;
                        case 15:
                            Uri _arg011 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            notifyTuned(_arg011);
                            break;
                        case 16:
                            int _arg012 = data.readInt();
                            String _arg14 = data.readString();
                            data.enforceNoDataAvail();
                            notifyTrackSelected(_arg012, _arg14);
                            break;
                        case 17:
                            List<TvTrackInfo> _arg013 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                            data.enforceNoDataAvail();
                            notifyTracksChanged(_arg013);
                            break;
                        case 18:
                            notifyVideoAvailable();
                            break;
                        case 19:
                            int _arg014 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyVideoUnavailable(_arg014);
                            break;
                        case 20:
                            notifyContentAllowed();
                            break;
                        case 21:
                            String _arg015 = data.readString();
                            data.enforceNoDataAvail();
                            notifyContentBlocked(_arg015);
                            break;
                        case 22:
                            int _arg016 = data.readInt();
                            data.enforceNoDataAvail();
                            notifySignalStrength(_arg016);
                            break;
                        case 23:
                            Surface _arg017 = (Surface) data.readTypedObject(Surface.CREATOR);
                            data.enforceNoDataAvail();
                            setSurface(_arg017);
                            break;
                        case 24:
                            int _arg018 = data.readInt();
                            int _arg15 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            dispatchSurfaceChanged(_arg018, _arg15, _arg2);
                            break;
                        case 25:
                            BroadcastInfoResponse _arg019 = (BroadcastInfoResponse) data.readTypedObject(BroadcastInfoResponse.CREATOR);
                            data.enforceNoDataAvail();
                            notifyBroadcastInfoResponse(_arg019);
                            break;
                        case 26:
                            AdResponse _arg020 = (AdResponse) data.readTypedObject(AdResponse.CREATOR);
                            data.enforceNoDataAvail();
                            notifyAdResponse(_arg020);
                            break;
                        case 27:
                            IBinder _arg021 = data.readStrongBinder();
                            Rect _arg16 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            createMediaView(_arg021, _arg16);
                            break;
                        case 28:
                            Rect _arg022 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            relayoutMediaView(_arg022);
                            break;
                        case 29:
                            removeMediaView();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITvInteractiveAppSession {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITvInteractiveAppSession.DESCRIPTOR;
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void startInteractiveApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void stopInteractiveApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void resetInteractiveApp() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void createBiInteractiveApp(Uri biIAppUri, Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(biIAppUri, 0);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void destroyBiInteractiveApp(String biIAppId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeString(biIAppId);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void setTeletextAppEnabled(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void sendCurrentChannelUri(Uri channelUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void sendCurrentChannelLcn(int lcn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeInt(lcn);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void sendStreamVolume(float volume) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeFloat(volume);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void sendTrackInfoList(List<TvTrackInfo> tracks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedList(tracks);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void sendCurrentTvInputId(String inputId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeString(inputId);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void sendSigningResult(String signingId, byte[] result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeString(signingId);
                    _data.writeByteArray(result);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyError(String errMsg, Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeString(errMsg);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void release() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyTuned(Uri channelUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyTrackSelected(int type, String trackId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(trackId);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyTracksChanged(List<TvTrackInfo> tracks) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedList(tracks);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyVideoAvailable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyVideoUnavailable(int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeInt(reason);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyContentAllowed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyContentBlocked(String rating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeString(rating);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifySignalStrength(int strength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeInt(strength);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void setSurface(Surface surface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(surface, 0);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void dispatchSurfaceChanged(int format, int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeInt(format);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyBroadcastInfoResponse(BroadcastInfoResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void notifyAdResponse(AdResponse response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(response, 0);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void createMediaView(IBinder windowToken, Rect frame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeTypedObject(frame, 0);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void relayoutMediaView(Rect frame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    _data.writeTypedObject(frame, 0);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppSession
            public void removeMediaView() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppSession.DESCRIPTOR);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 28;
        }
    }
}
