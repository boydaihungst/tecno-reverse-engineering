package android.media.tv.interactive;

import android.graphics.Rect;
import android.media.tv.AdResponse;
import android.media.tv.BroadcastInfoResponse;
import android.media.tv.TvTrackInfo;
import android.media.tv.interactive.ITvInteractiveAppClient;
import android.media.tv.interactive.ITvInteractiveAppManagerCallback;
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
public interface ITvInteractiveAppManager extends IInterface {
    public static final String DESCRIPTOR = "android.media.tv.interactive.ITvInteractiveAppManager";

    void createBiInteractiveApp(IBinder iBinder, Uri uri, Bundle bundle, int i) throws RemoteException;

    void createMediaView(IBinder iBinder, IBinder iBinder2, Rect rect, int i) throws RemoteException;

    void createSession(ITvInteractiveAppClient iTvInteractiveAppClient, String str, int i, int i2, int i3) throws RemoteException;

    void destroyBiInteractiveApp(IBinder iBinder, String str, int i) throws RemoteException;

    void dispatchSurfaceChanged(IBinder iBinder, int i, int i2, int i3, int i4) throws RemoteException;

    List<TvInteractiveAppServiceInfo> getTvInteractiveAppServiceList(int i) throws RemoteException;

    void notifyAdResponse(IBinder iBinder, AdResponse adResponse, int i) throws RemoteException;

    void notifyBroadcastInfoResponse(IBinder iBinder, BroadcastInfoResponse broadcastInfoResponse, int i) throws RemoteException;

    void notifyContentAllowed(IBinder iBinder, int i) throws RemoteException;

    void notifyContentBlocked(IBinder iBinder, String str, int i) throws RemoteException;

    void notifyError(IBinder iBinder, String str, Bundle bundle, int i) throws RemoteException;

    void notifySignalStrength(IBinder iBinder, int i, int i2) throws RemoteException;

    void notifyTrackSelected(IBinder iBinder, int i, String str, int i2) throws RemoteException;

    void notifyTracksChanged(IBinder iBinder, List<TvTrackInfo> list, int i) throws RemoteException;

    void notifyTuned(IBinder iBinder, Uri uri, int i) throws RemoteException;

    void notifyVideoAvailable(IBinder iBinder, int i) throws RemoteException;

    void notifyVideoUnavailable(IBinder iBinder, int i, int i2) throws RemoteException;

    void registerAppLinkInfo(String str, AppLinkInfo appLinkInfo, int i) throws RemoteException;

    void registerCallback(ITvInteractiveAppManagerCallback iTvInteractiveAppManagerCallback, int i) throws RemoteException;

    void relayoutMediaView(IBinder iBinder, Rect rect, int i) throws RemoteException;

    void releaseSession(IBinder iBinder, int i) throws RemoteException;

    void removeMediaView(IBinder iBinder, int i) throws RemoteException;

    void resetInteractiveApp(IBinder iBinder, int i) throws RemoteException;

    void sendAppLinkCommand(String str, Bundle bundle, int i) throws RemoteException;

    void sendCurrentChannelLcn(IBinder iBinder, int i, int i2) throws RemoteException;

    void sendCurrentChannelUri(IBinder iBinder, Uri uri, int i) throws RemoteException;

    void sendCurrentTvInputId(IBinder iBinder, String str, int i) throws RemoteException;

    void sendSigningResult(IBinder iBinder, String str, byte[] bArr, int i) throws RemoteException;

    void sendStreamVolume(IBinder iBinder, float f, int i) throws RemoteException;

    void sendTrackInfoList(IBinder iBinder, List<TvTrackInfo> list, int i) throws RemoteException;

    void setSurface(IBinder iBinder, Surface surface, int i) throws RemoteException;

    void setTeletextAppEnabled(IBinder iBinder, boolean z, int i) throws RemoteException;

    void startInteractiveApp(IBinder iBinder, int i) throws RemoteException;

    void stopInteractiveApp(IBinder iBinder, int i) throws RemoteException;

    void unregisterAppLinkInfo(String str, AppLinkInfo appLinkInfo, int i) throws RemoteException;

    void unregisterCallback(ITvInteractiveAppManagerCallback iTvInteractiveAppManagerCallback, int i) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements ITvInteractiveAppManager {
        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public List<TvInteractiveAppServiceInfo> getTvInteractiveAppServiceList(int userId) throws RemoteException {
            return null;
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void registerAppLinkInfo(String tiasId, AppLinkInfo info, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void unregisterAppLinkInfo(String tiasId, AppLinkInfo info, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendAppLinkCommand(String tiasId, Bundle command, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void startInteractiveApp(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void stopInteractiveApp(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void resetInteractiveApp(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void createBiInteractiveApp(IBinder sessionToken, Uri biIAppUri, Bundle params, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void destroyBiInteractiveApp(IBinder sessionToken, String biIAppId, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void setTeletextAppEnabled(IBinder sessionToken, boolean enable, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendCurrentChannelUri(IBinder sessionToken, Uri channelUri, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendCurrentChannelLcn(IBinder sessionToken, int lcn, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendStreamVolume(IBinder sessionToken, float volume, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendTrackInfoList(IBinder sessionToken, List<TvTrackInfo> tracks, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendCurrentTvInputId(IBinder sessionToken, String inputId, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void sendSigningResult(IBinder sessionToken, String signingId, byte[] result, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyError(IBinder sessionToken, String errMsg, Bundle params, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void createSession(ITvInteractiveAppClient client, String iAppServiceId, int type, int seq, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void releaseSession(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyTuned(IBinder sessionToken, Uri channelUri, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyTrackSelected(IBinder sessionToken, int type, String trackId, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyTracksChanged(IBinder sessionToken, List<TvTrackInfo> tracks, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyVideoAvailable(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyVideoUnavailable(IBinder sessionToken, int reason, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyContentAllowed(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyContentBlocked(IBinder sessionToken, String rating, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifySignalStrength(IBinder sessionToken, int stength, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void setSurface(IBinder sessionToken, Surface surface, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void dispatchSurfaceChanged(IBinder sessionToken, int format, int width, int height, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyBroadcastInfoResponse(IBinder sessionToken, BroadcastInfoResponse response, int UserId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void notifyAdResponse(IBinder sessionToken, AdResponse response, int UserId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void createMediaView(IBinder sessionToken, IBinder windowToken, Rect frame, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void relayoutMediaView(IBinder sessionToken, Rect frame, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void removeMediaView(IBinder sessionToken, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void registerCallback(ITvInteractiveAppManagerCallback callback, int userId) throws RemoteException {
        }

        @Override // android.media.tv.interactive.ITvInteractiveAppManager
        public void unregisterCallback(ITvInteractiveAppManagerCallback callback, int userId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements ITvInteractiveAppManager {
        static final int TRANSACTION_createBiInteractiveApp = 8;
        static final int TRANSACTION_createMediaView = 32;
        static final int TRANSACTION_createSession = 18;
        static final int TRANSACTION_destroyBiInteractiveApp = 9;
        static final int TRANSACTION_dispatchSurfaceChanged = 29;
        static final int TRANSACTION_getTvInteractiveAppServiceList = 1;
        static final int TRANSACTION_notifyAdResponse = 31;
        static final int TRANSACTION_notifyBroadcastInfoResponse = 30;
        static final int TRANSACTION_notifyContentAllowed = 25;
        static final int TRANSACTION_notifyContentBlocked = 26;
        static final int TRANSACTION_notifyError = 17;
        static final int TRANSACTION_notifySignalStrength = 27;
        static final int TRANSACTION_notifyTrackSelected = 21;
        static final int TRANSACTION_notifyTracksChanged = 22;
        static final int TRANSACTION_notifyTuned = 20;
        static final int TRANSACTION_notifyVideoAvailable = 23;
        static final int TRANSACTION_notifyVideoUnavailable = 24;
        static final int TRANSACTION_registerAppLinkInfo = 2;
        static final int TRANSACTION_registerCallback = 35;
        static final int TRANSACTION_relayoutMediaView = 33;
        static final int TRANSACTION_releaseSession = 19;
        static final int TRANSACTION_removeMediaView = 34;
        static final int TRANSACTION_resetInteractiveApp = 7;
        static final int TRANSACTION_sendAppLinkCommand = 4;
        static final int TRANSACTION_sendCurrentChannelLcn = 12;
        static final int TRANSACTION_sendCurrentChannelUri = 11;
        static final int TRANSACTION_sendCurrentTvInputId = 15;
        static final int TRANSACTION_sendSigningResult = 16;
        static final int TRANSACTION_sendStreamVolume = 13;
        static final int TRANSACTION_sendTrackInfoList = 14;
        static final int TRANSACTION_setSurface = 28;
        static final int TRANSACTION_setTeletextAppEnabled = 10;
        static final int TRANSACTION_startInteractiveApp = 5;
        static final int TRANSACTION_stopInteractiveApp = 6;
        static final int TRANSACTION_unregisterAppLinkInfo = 3;
        static final int TRANSACTION_unregisterCallback = 36;

        public Stub() {
            attachInterface(this, ITvInteractiveAppManager.DESCRIPTOR);
        }

        public static ITvInteractiveAppManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ITvInteractiveAppManager.DESCRIPTOR);
            if (iin != null && (iin instanceof ITvInteractiveAppManager)) {
                return (ITvInteractiveAppManager) iin;
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
                    return "getTvInteractiveAppServiceList";
                case 2:
                    return "registerAppLinkInfo";
                case 3:
                    return "unregisterAppLinkInfo";
                case 4:
                    return "sendAppLinkCommand";
                case 5:
                    return "startInteractiveApp";
                case 6:
                    return "stopInteractiveApp";
                case 7:
                    return "resetInteractiveApp";
                case 8:
                    return "createBiInteractiveApp";
                case 9:
                    return "destroyBiInteractiveApp";
                case 10:
                    return "setTeletextAppEnabled";
                case 11:
                    return "sendCurrentChannelUri";
                case 12:
                    return "sendCurrentChannelLcn";
                case 13:
                    return "sendStreamVolume";
                case 14:
                    return "sendTrackInfoList";
                case 15:
                    return "sendCurrentTvInputId";
                case 16:
                    return "sendSigningResult";
                case 17:
                    return "notifyError";
                case 18:
                    return "createSession";
                case 19:
                    return "releaseSession";
                case 20:
                    return "notifyTuned";
                case 21:
                    return "notifyTrackSelected";
                case 22:
                    return "notifyTracksChanged";
                case 23:
                    return "notifyVideoAvailable";
                case 24:
                    return "notifyVideoUnavailable";
                case 25:
                    return "notifyContentAllowed";
                case 26:
                    return "notifyContentBlocked";
                case 27:
                    return "notifySignalStrength";
                case 28:
                    return "setSurface";
                case 29:
                    return "dispatchSurfaceChanged";
                case 30:
                    return "notifyBroadcastInfoResponse";
                case 31:
                    return "notifyAdResponse";
                case 32:
                    return "createMediaView";
                case 33:
                    return "relayoutMediaView";
                case 34:
                    return "removeMediaView";
                case 35:
                    return "registerCallback";
                case 36:
                    return "unregisterCallback";
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
                data.enforceInterface(ITvInteractiveAppManager.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ITvInteractiveAppManager.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            List<TvInteractiveAppServiceInfo> _result = getTvInteractiveAppServiceList(_arg0);
                            reply.writeNoException();
                            reply.writeTypedList(_result);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            AppLinkInfo _arg1 = (AppLinkInfo) data.readTypedObject(AppLinkInfo.CREATOR);
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            registerAppLinkInfo(_arg02, _arg1, _arg2);
                            reply.writeNoException();
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            AppLinkInfo _arg12 = (AppLinkInfo) data.readTypedObject(AppLinkInfo.CREATOR);
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            unregisterAppLinkInfo(_arg03, _arg12, _arg22);
                            reply.writeNoException();
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            Bundle _arg13 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            sendAppLinkCommand(_arg04, _arg13, _arg23);
                            reply.writeNoException();
                            break;
                        case 5:
                            IBinder _arg05 = data.readStrongBinder();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            startInteractiveApp(_arg05, _arg14);
                            reply.writeNoException();
                            break;
                        case 6:
                            IBinder _arg06 = data.readStrongBinder();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            stopInteractiveApp(_arg06, _arg15);
                            reply.writeNoException();
                            break;
                        case 7:
                            IBinder _arg07 = data.readStrongBinder();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            resetInteractiveApp(_arg07, _arg16);
                            reply.writeNoException();
                            break;
                        case 8:
                            IBinder _arg08 = data.readStrongBinder();
                            Uri _arg17 = (Uri) data.readTypedObject(Uri.CREATOR);
                            Bundle _arg24 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            createBiInteractiveApp(_arg08, _arg17, _arg24, _arg3);
                            reply.writeNoException();
                            break;
                        case 9:
                            IBinder _arg09 = data.readStrongBinder();
                            String _arg18 = data.readString();
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            destroyBiInteractiveApp(_arg09, _arg18, _arg25);
                            reply.writeNoException();
                            break;
                        case 10:
                            IBinder _arg010 = data.readStrongBinder();
                            boolean _arg19 = data.readBoolean();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            setTeletextAppEnabled(_arg010, _arg19, _arg26);
                            reply.writeNoException();
                            break;
                        case 11:
                            IBinder _arg011 = data.readStrongBinder();
                            Uri _arg110 = (Uri) data.readTypedObject(Uri.CREATOR);
                            int _arg27 = data.readInt();
                            data.enforceNoDataAvail();
                            sendCurrentChannelUri(_arg011, _arg110, _arg27);
                            reply.writeNoException();
                            break;
                        case 12:
                            IBinder _arg012 = data.readStrongBinder();
                            int _arg111 = data.readInt();
                            int _arg28 = data.readInt();
                            data.enforceNoDataAvail();
                            sendCurrentChannelLcn(_arg012, _arg111, _arg28);
                            reply.writeNoException();
                            break;
                        case 13:
                            IBinder _arg013 = data.readStrongBinder();
                            float _arg112 = data.readFloat();
                            int _arg29 = data.readInt();
                            data.enforceNoDataAvail();
                            sendStreamVolume(_arg013, _arg112, _arg29);
                            reply.writeNoException();
                            break;
                        case 14:
                            IBinder _arg014 = data.readStrongBinder();
                            List<TvTrackInfo> _arg113 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                            int _arg210 = data.readInt();
                            data.enforceNoDataAvail();
                            sendTrackInfoList(_arg014, _arg113, _arg210);
                            reply.writeNoException();
                            break;
                        case 15:
                            IBinder _arg015 = data.readStrongBinder();
                            String _arg114 = data.readString();
                            int _arg211 = data.readInt();
                            data.enforceNoDataAvail();
                            sendCurrentTvInputId(_arg015, _arg114, _arg211);
                            reply.writeNoException();
                            break;
                        case 16:
                            IBinder _arg016 = data.readStrongBinder();
                            String _arg115 = data.readString();
                            byte[] _arg212 = data.createByteArray();
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            sendSigningResult(_arg016, _arg115, _arg212, _arg32);
                            reply.writeNoException();
                            break;
                        case 17:
                            IBinder _arg017 = data.readStrongBinder();
                            String _arg116 = data.readString();
                            Bundle _arg213 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            int _arg33 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyError(_arg017, _arg116, _arg213, _arg33);
                            reply.writeNoException();
                            break;
                        case 18:
                            IBinder _arg018 = data.readStrongBinder();
                            ITvInteractiveAppClient _arg019 = ITvInteractiveAppClient.Stub.asInterface(_arg018);
                            String _arg117 = data.readString();
                            int _arg214 = data.readInt();
                            int _arg34 = data.readInt();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            createSession(_arg019, _arg117, _arg214, _arg34, _arg4);
                            reply.writeNoException();
                            break;
                        case 19:
                            IBinder _arg020 = data.readStrongBinder();
                            int _arg118 = data.readInt();
                            data.enforceNoDataAvail();
                            releaseSession(_arg020, _arg118);
                            reply.writeNoException();
                            break;
                        case 20:
                            IBinder _arg021 = data.readStrongBinder();
                            Uri _arg119 = (Uri) data.readTypedObject(Uri.CREATOR);
                            int _arg215 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyTuned(_arg021, _arg119, _arg215);
                            reply.writeNoException();
                            break;
                        case 21:
                            IBinder _arg022 = data.readStrongBinder();
                            int _arg120 = data.readInt();
                            String _arg216 = data.readString();
                            int _arg35 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyTrackSelected(_arg022, _arg120, _arg216, _arg35);
                            reply.writeNoException();
                            break;
                        case 22:
                            IBinder _arg023 = data.readStrongBinder();
                            List<TvTrackInfo> _arg121 = data.createTypedArrayList(TvTrackInfo.CREATOR);
                            int _arg217 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyTracksChanged(_arg023, _arg121, _arg217);
                            reply.writeNoException();
                            break;
                        case 23:
                            IBinder _arg024 = data.readStrongBinder();
                            int _arg122 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyVideoAvailable(_arg024, _arg122);
                            reply.writeNoException();
                            break;
                        case 24:
                            IBinder _arg025 = data.readStrongBinder();
                            int _arg123 = data.readInt();
                            int _arg218 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyVideoUnavailable(_arg025, _arg123, _arg218);
                            reply.writeNoException();
                            break;
                        case 25:
                            IBinder _arg026 = data.readStrongBinder();
                            int _arg124 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyContentAllowed(_arg026, _arg124);
                            reply.writeNoException();
                            break;
                        case 26:
                            IBinder _arg027 = data.readStrongBinder();
                            String _arg125 = data.readString();
                            int _arg219 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyContentBlocked(_arg027, _arg125, _arg219);
                            reply.writeNoException();
                            break;
                        case 27:
                            IBinder _arg028 = data.readStrongBinder();
                            int _arg126 = data.readInt();
                            int _arg220 = data.readInt();
                            data.enforceNoDataAvail();
                            notifySignalStrength(_arg028, _arg126, _arg220);
                            reply.writeNoException();
                            break;
                        case 28:
                            IBinder _arg029 = data.readStrongBinder();
                            Surface _arg127 = (Surface) data.readTypedObject(Surface.CREATOR);
                            int _arg221 = data.readInt();
                            data.enforceNoDataAvail();
                            setSurface(_arg029, _arg127, _arg221);
                            reply.writeNoException();
                            break;
                        case 29:
                            IBinder _arg030 = data.readStrongBinder();
                            int _arg128 = data.readInt();
                            int _arg222 = data.readInt();
                            int _arg36 = data.readInt();
                            int _arg42 = data.readInt();
                            data.enforceNoDataAvail();
                            dispatchSurfaceChanged(_arg030, _arg128, _arg222, _arg36, _arg42);
                            reply.writeNoException();
                            break;
                        case 30:
                            IBinder _arg031 = data.readStrongBinder();
                            BroadcastInfoResponse _arg129 = (BroadcastInfoResponse) data.readTypedObject(BroadcastInfoResponse.CREATOR);
                            int _arg223 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyBroadcastInfoResponse(_arg031, _arg129, _arg223);
                            reply.writeNoException();
                            break;
                        case 31:
                            IBinder _arg032 = data.readStrongBinder();
                            AdResponse _arg130 = (AdResponse) data.readTypedObject(AdResponse.CREATOR);
                            int _arg224 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyAdResponse(_arg032, _arg130, _arg224);
                            reply.writeNoException();
                            break;
                        case 32:
                            IBinder _arg033 = data.readStrongBinder();
                            IBinder _arg131 = data.readStrongBinder();
                            Rect _arg225 = (Rect) data.readTypedObject(Rect.CREATOR);
                            int _arg37 = data.readInt();
                            data.enforceNoDataAvail();
                            createMediaView(_arg033, _arg131, _arg225, _arg37);
                            reply.writeNoException();
                            break;
                        case 33:
                            IBinder _arg034 = data.readStrongBinder();
                            Rect _arg132 = (Rect) data.readTypedObject(Rect.CREATOR);
                            int _arg226 = data.readInt();
                            data.enforceNoDataAvail();
                            relayoutMediaView(_arg034, _arg132, _arg226);
                            reply.writeNoException();
                            break;
                        case 34:
                            IBinder _arg035 = data.readStrongBinder();
                            int _arg133 = data.readInt();
                            data.enforceNoDataAvail();
                            removeMediaView(_arg035, _arg133);
                            reply.writeNoException();
                            break;
                        case 35:
                            ITvInteractiveAppManagerCallback _arg036 = ITvInteractiveAppManagerCallback.Stub.asInterface(data.readStrongBinder());
                            int _arg134 = data.readInt();
                            data.enforceNoDataAvail();
                            registerCallback(_arg036, _arg134);
                            reply.writeNoException();
                            break;
                        case 36:
                            ITvInteractiveAppManagerCallback _arg037 = ITvInteractiveAppManagerCallback.Stub.asInterface(data.readStrongBinder());
                            int _arg135 = data.readInt();
                            data.enforceNoDataAvail();
                            unregisterCallback(_arg037, _arg135);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class Proxy implements ITvInteractiveAppManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITvInteractiveAppManager.DESCRIPTOR;
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public List<TvInteractiveAppServiceInfo> getTvInteractiveAppServiceList(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<TvInteractiveAppServiceInfo> _result = _reply.createTypedArrayList(TvInteractiveAppServiceInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void registerAppLinkInfo(String tiasId, AppLinkInfo info, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeString(tiasId);
                    _data.writeTypedObject(info, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void unregisterAppLinkInfo(String tiasId, AppLinkInfo info, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeString(tiasId);
                    _data.writeTypedObject(info, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendAppLinkCommand(String tiasId, Bundle command, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeString(tiasId);
                    _data.writeTypedObject(command, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void startInteractiveApp(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void stopInteractiveApp(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void resetInteractiveApp(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void createBiInteractiveApp(IBinder sessionToken, Uri biIAppUri, Bundle params, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(biIAppUri, 0);
                    _data.writeTypedObject(params, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void destroyBiInteractiveApp(IBinder sessionToken, String biIAppId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeString(biIAppId);
                    _data.writeInt(userId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void setTeletextAppEnabled(IBinder sessionToken, boolean enable, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeBoolean(enable);
                    _data.writeInt(userId);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendCurrentChannelUri(IBinder sessionToken, Uri channelUri, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(channelUri, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendCurrentChannelLcn(IBinder sessionToken, int lcn, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(lcn);
                    _data.writeInt(userId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendStreamVolume(IBinder sessionToken, float volume, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeFloat(volume);
                    _data.writeInt(userId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendTrackInfoList(IBinder sessionToken, List<TvTrackInfo> tracks, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedList(tracks);
                    _data.writeInt(userId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendCurrentTvInputId(IBinder sessionToken, String inputId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeString(inputId);
                    _data.writeInt(userId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void sendSigningResult(IBinder sessionToken, String signingId, byte[] result, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeString(signingId);
                    _data.writeByteArray(result);
                    _data.writeInt(userId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyError(IBinder sessionToken, String errMsg, Bundle params, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeString(errMsg);
                    _data.writeTypedObject(params, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void createSession(ITvInteractiveAppClient client, String iAppServiceId, int type, int seq, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeString(iAppServiceId);
                    _data.writeInt(type);
                    _data.writeInt(seq);
                    _data.writeInt(userId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void releaseSession(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyTuned(IBinder sessionToken, Uri channelUri, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(channelUri, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyTrackSelected(IBinder sessionToken, int type, String trackId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(type);
                    _data.writeString(trackId);
                    _data.writeInt(userId);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyTracksChanged(IBinder sessionToken, List<TvTrackInfo> tracks, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedList(tracks);
                    _data.writeInt(userId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyVideoAvailable(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyVideoUnavailable(IBinder sessionToken, int reason, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(reason);
                    _data.writeInt(userId);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyContentAllowed(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyContentBlocked(IBinder sessionToken, String rating, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeString(rating);
                    _data.writeInt(userId);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifySignalStrength(IBinder sessionToken, int stength, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(stength);
                    _data.writeInt(userId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void setSurface(IBinder sessionToken, Surface surface, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(surface, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void dispatchSurfaceChanged(IBinder sessionToken, int format, int width, int height, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(format);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeInt(userId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyBroadcastInfoResponse(IBinder sessionToken, BroadcastInfoResponse response, int UserId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(response, 0);
                    _data.writeInt(UserId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void notifyAdResponse(IBinder sessionToken, AdResponse response, int UserId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(response, 0);
                    _data.writeInt(UserId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void createMediaView(IBinder sessionToken, IBinder windowToken, Rect frame, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeStrongBinder(windowToken);
                    _data.writeTypedObject(frame, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void relayoutMediaView(IBinder sessionToken, Rect frame, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeTypedObject(frame, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void removeMediaView(IBinder sessionToken, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongBinder(sessionToken);
                    _data.writeInt(userId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void registerCallback(ITvInteractiveAppManagerCallback callback, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    _data.writeInt(userId);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.media.tv.interactive.ITvInteractiveAppManager
            public void unregisterCallback(ITvInteractiveAppManagerCallback callback, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ITvInteractiveAppManager.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    _data.writeInt(userId);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 35;
        }
    }
}
