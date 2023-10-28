package android.media.tv;

import android.graphics.Rect;
import android.media.PlaybackParams;
import android.media.tv.interactive.TvInteractiveAppService;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
/* loaded from: classes2.dex */
public interface ITvInputSession extends IInterface {
    void appPrivateCommand(String str, Bundle bundle) throws RemoteException;

    void createOverlayView(IBinder iBinder, Rect rect) throws RemoteException;

    void dispatchSurfaceChanged(int i, int i2, int i3) throws RemoteException;

    void pauseRecording(Bundle bundle) throws RemoteException;

    void relayoutOverlayView(Rect rect) throws RemoteException;

    void release() throws RemoteException;

    void removeBroadcastInfo(int i) throws RemoteException;

    void removeOverlayView() throws RemoteException;

    void requestAd(AdRequest adRequest) throws RemoteException;

    void requestBroadcastInfo(BroadcastInfoRequest broadcastInfoRequest) throws RemoteException;

    void resumeRecording(Bundle bundle) throws RemoteException;

    void selectTrack(int i, String str) throws RemoteException;

    void setCaptionEnabled(boolean z) throws RemoteException;

    void setInteractiveAppNotificationEnabled(boolean z) throws RemoteException;

    void setMain(boolean z) throws RemoteException;

    void setSurface(Surface surface) throws RemoteException;

    void setVolume(float f) throws RemoteException;

    void startRecording(Uri uri, Bundle bundle) throws RemoteException;

    void stopRecording() throws RemoteException;

    void timeShiftEnablePositionTracking(boolean z) throws RemoteException;

    void timeShiftPause() throws RemoteException;

    void timeShiftPlay(Uri uri) throws RemoteException;

    void timeShiftResume() throws RemoteException;

    void timeShiftSeekTo(long j) throws RemoteException;

    void timeShiftSetPlaybackParams(PlaybackParams playbackParams) throws RemoteException;

    void tune(Uri uri, Bundle bundle) throws RemoteException;

    void unblockContent(String str) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInputSession {
        @Override // android.media.tv.ITvInputSession
        public void release() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void setMain(boolean isMain) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void setSurface(Surface surface) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void dispatchSurfaceChanged(int format, int width, int height) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void setVolume(float volume) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void tune(Uri channelUri, Bundle params) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void setCaptionEnabled(boolean enabled) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void selectTrack(int type, String trackId) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void setInteractiveAppNotificationEnabled(boolean enable) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void appPrivateCommand(String action, Bundle data) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void createOverlayView(IBinder windowToken, Rect frame) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void relayoutOverlayView(Rect frame) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void removeOverlayView() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void unblockContent(String unblockedRating) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void timeShiftPlay(Uri recordedProgramUri) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void timeShiftPause() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void timeShiftResume() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void timeShiftSeekTo(long timeMs) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void timeShiftSetPlaybackParams(PlaybackParams params) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void timeShiftEnablePositionTracking(boolean enable) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void startRecording(Uri programUri, Bundle params) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void stopRecording() throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void pauseRecording(Bundle params) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void resumeRecording(Bundle params) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void requestBroadcastInfo(BroadcastInfoRequest request) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void removeBroadcastInfo(int id) throws RemoteException {
        }

        @Override // android.media.tv.ITvInputSession
        public void requestAd(AdRequest request) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInputSession {
        public static final String DESCRIPTOR = "android.media.tv.ITvInputSession";
        static final int TRANSACTION_appPrivateCommand = 10;
        static final int TRANSACTION_createOverlayView = 11;
        static final int TRANSACTION_dispatchSurfaceChanged = 4;
        static final int TRANSACTION_pauseRecording = 23;
        static final int TRANSACTION_relayoutOverlayView = 12;
        static final int TRANSACTION_release = 1;
        static final int TRANSACTION_removeBroadcastInfo = 26;
        static final int TRANSACTION_removeOverlayView = 13;
        static final int TRANSACTION_requestAd = 27;
        static final int TRANSACTION_requestBroadcastInfo = 25;
        static final int TRANSACTION_resumeRecording = 24;
        static final int TRANSACTION_selectTrack = 8;
        static final int TRANSACTION_setCaptionEnabled = 7;
        static final int TRANSACTION_setInteractiveAppNotificationEnabled = 9;
        static final int TRANSACTION_setMain = 2;
        static final int TRANSACTION_setSurface = 3;
        static final int TRANSACTION_setVolume = 5;
        static final int TRANSACTION_startRecording = 21;
        static final int TRANSACTION_stopRecording = 22;
        static final int TRANSACTION_timeShiftEnablePositionTracking = 20;
        static final int TRANSACTION_timeShiftPause = 16;
        static final int TRANSACTION_timeShiftPlay = 15;
        static final int TRANSACTION_timeShiftResume = 17;
        static final int TRANSACTION_timeShiftSeekTo = 18;
        static final int TRANSACTION_timeShiftSetPlaybackParams = 19;
        static final int TRANSACTION_tune = 6;
        static final int TRANSACTION_unblockContent = 14;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITvInputSession asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInputSession)) {
                return (ITvInputSession) iin;
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
                    return "release";
                case 2:
                    return "setMain";
                case 3:
                    return "setSurface";
                case 4:
                    return "dispatchSurfaceChanged";
                case 5:
                    return "setVolume";
                case 6:
                    return TvInteractiveAppService.PLAYBACK_COMMAND_TYPE_TUNE;
                case 7:
                    return "setCaptionEnabled";
                case 8:
                    return "selectTrack";
                case 9:
                    return "setInteractiveAppNotificationEnabled";
                case 10:
                    return "appPrivateCommand";
                case 11:
                    return "createOverlayView";
                case 12:
                    return "relayoutOverlayView";
                case 13:
                    return "removeOverlayView";
                case 14:
                    return "unblockContent";
                case 15:
                    return "timeShiftPlay";
                case 16:
                    return "timeShiftPause";
                case 17:
                    return "timeShiftResume";
                case 18:
                    return "timeShiftSeekTo";
                case 19:
                    return "timeShiftSetPlaybackParams";
                case 20:
                    return "timeShiftEnablePositionTracking";
                case 21:
                    return "startRecording";
                case 22:
                    return "stopRecording";
                case 23:
                    return "pauseRecording";
                case 24:
                    return "resumeRecording";
                case 25:
                    return "requestBroadcastInfo";
                case 26:
                    return "removeBroadcastInfo";
                case 27:
                    return "requestAd";
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
                            release();
                            break;
                        case 2:
                            boolean _arg0 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setMain(_arg0);
                            break;
                        case 3:
                            Surface _arg02 = (Surface) data.readTypedObject(Surface.CREATOR);
                            data.enforceNoDataAvail();
                            setSurface(_arg02);
                            break;
                        case 4:
                            int _arg03 = data.readInt();
                            int _arg1 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            dispatchSurfaceChanged(_arg03, _arg1, _arg2);
                            break;
                        case 5:
                            float _arg04 = data.readFloat();
                            data.enforceNoDataAvail();
                            setVolume(_arg04);
                            break;
                        case 6:
                            Uri _arg05 = (Uri) data.readTypedObject(Uri.CREATOR);
                            Bundle _arg12 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            tune(_arg05, _arg12);
                            break;
                        case 7:
                            boolean _arg06 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setCaptionEnabled(_arg06);
                            break;
                        case 8:
                            int _arg07 = data.readInt();
                            String _arg13 = data.readString();
                            data.enforceNoDataAvail();
                            selectTrack(_arg07, _arg13);
                            break;
                        case 9:
                            boolean _arg08 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setInteractiveAppNotificationEnabled(_arg08);
                            break;
                        case 10:
                            String _arg09 = data.readString();
                            Bundle _arg14 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            appPrivateCommand(_arg09, _arg14);
                            break;
                        case 11:
                            IBinder _arg010 = data.readStrongBinder();
                            Rect _arg15 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            createOverlayView(_arg010, _arg15);
                            break;
                        case 12:
                            Rect _arg011 = (Rect) data.readTypedObject(Rect.CREATOR);
                            data.enforceNoDataAvail();
                            relayoutOverlayView(_arg011);
                            break;
                        case 13:
                            removeOverlayView();
                            break;
                        case 14:
                            String _arg012 = data.readString();
                            data.enforceNoDataAvail();
                            unblockContent(_arg012);
                            break;
                        case 15:
                            Uri _arg013 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            timeShiftPlay(_arg013);
                            break;
                        case 16:
                            timeShiftPause();
                            break;
                        case 17:
                            timeShiftResume();
                            break;
                        case 18:
                            long _arg014 = data.readLong();
                            data.enforceNoDataAvail();
                            timeShiftSeekTo(_arg014);
                            break;
                        case 19:
                            PlaybackParams _arg015 = (PlaybackParams) data.readTypedObject(PlaybackParams.CREATOR);
                            data.enforceNoDataAvail();
                            timeShiftSetPlaybackParams(_arg015);
                            break;
                        case 20:
                            boolean _arg016 = data.readBoolean();
                            data.enforceNoDataAvail();
                            timeShiftEnablePositionTracking(_arg016);
                            break;
                        case 21:
                            Uri _arg017 = (Uri) data.readTypedObject(Uri.CREATOR);
                            Bundle _arg16 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            startRecording(_arg017, _arg16);
                            break;
                        case 22:
                            stopRecording();
                            break;
                        case 23:
                            Bundle _arg018 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            pauseRecording(_arg018);
                            break;
                        case 24:
                            Bundle _arg019 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            resumeRecording(_arg019);
                            break;
                        case 25:
                            BroadcastInfoRequest _arg020 = (BroadcastInfoRequest) data.readTypedObject(BroadcastInfoRequest.CREATOR);
                            data.enforceNoDataAvail();
                            requestBroadcastInfo(_arg020);
                            break;
                        case 26:
                            int _arg021 = data.readInt();
                            data.enforceNoDataAvail();
                            removeBroadcastInfo(_arg021);
                            break;
                        case 27:
                            AdRequest _arg022 = (AdRequest) data.readTypedObject(AdRequest.CREATOR);
                            data.enforceNoDataAvail();
                            requestAd(_arg022);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements ITvInputSession {
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

            @Override // android.media.tv.ITvInputSession
            public void release() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void setMain(boolean isMain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isMain);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void setSurface(Surface surface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(surface, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void dispatchSurfaceChanged(int format, int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(format);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void setVolume(float volume) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(volume);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void tune(Uri channelUri, Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(channelUri, 0);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void setCaptionEnabled(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void selectTrack(int type, String trackId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(trackId);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void setInteractiveAppNotificationEnabled(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void appPrivateCommand(String action, Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeTypedObject(data, 0);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void createOverlayView(IBinder windowToken, Rect frame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeTypedObject(frame, 0);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void relayoutOverlayView(Rect frame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(frame, 0);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void removeOverlayView() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void unblockContent(String unblockedRating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(unblockedRating);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void timeShiftPlay(Uri recordedProgramUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(recordedProgramUri, 0);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void timeShiftPause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void timeShiftResume() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void timeShiftSeekTo(long timeMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(timeMs);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void timeShiftSetPlaybackParams(PlaybackParams params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void timeShiftEnablePositionTracking(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void startRecording(Uri programUri, Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(programUri, 0);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void stopRecording() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void pauseRecording(Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void resumeRecording(Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void requestBroadcastInfo(BroadcastInfoRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void removeBroadcastInfo(int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(id);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.media.tv.ITvInputSession
            public void requestAd(AdRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 26;
        }
    }
}
