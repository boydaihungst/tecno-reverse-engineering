package com.android.internal.telecom;

import android.media.MediaMetrics;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.PhoneAccountHandle;
import android.telephony.ims.ImsCallProfile;
import java.util.List;
/* loaded from: classes4.dex */
public interface IInCallAdapter extends IInterface {
    void addConferenceParticipants(String str, List<Uri> list) throws RemoteException;

    void answerCall(String str, int i) throws RemoteException;

    void conference(String str, String str2) throws RemoteException;

    void consultativeTransfer(String str, String str2) throws RemoteException;

    void deflectCall(String str, Uri uri) throws RemoteException;

    void disconnectCall(String str) throws RemoteException;

    void doTranAction(Bundle bundle) throws RemoteException;

    void enterBackgroundAudioProcessing(String str) throws RemoteException;

    void exitBackgroundAudioProcessing(String str, boolean z) throws RemoteException;

    void handoverTo(String str, PhoneAccountHandle phoneAccountHandle, int i, Bundle bundle) throws RemoteException;

    void holdCall(String str) throws RemoteException;

    void mergeConference(String str) throws RemoteException;

    void mute(boolean z) throws RemoteException;

    void phoneAccountSelected(String str, PhoneAccountHandle phoneAccountHandle, boolean z) throws RemoteException;

    void playDtmfTone(String str, char c) throws RemoteException;

    void postDialContinue(String str, boolean z) throws RemoteException;

    void pullExternalCall(String str) throws RemoteException;

    void putExtras(String str, Bundle bundle) throws RemoteException;

    void rejectCall(String str, boolean z, String str2) throws RemoteException;

    void rejectCallWithReason(String str, int i) throws RemoteException;

    void removeExtras(String str, List<String> list) throws RemoteException;

    void respondToRttRequest(String str, int i, boolean z) throws RemoteException;

    void sendCallEvent(String str, String str2, int i, Bundle bundle) throws RemoteException;

    void sendRttRequest(String str) throws RemoteException;

    void setAudioRoute(int i, String str) throws RemoteException;

    void setRttMode(String str, int i) throws RemoteException;

    void splitFromConference(String str) throws RemoteException;

    void stopDtmfTone(String str) throws RemoteException;

    void stopRtt(String str) throws RemoteException;

    void swapConference(String str) throws RemoteException;

    void transferCall(String str, Uri uri, boolean z) throws RemoteException;

    void turnOffProximitySensor(boolean z) throws RemoteException;

    void turnOnProximitySensor() throws RemoteException;

    void unholdCall(String str) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IInCallAdapter {
        @Override // com.android.internal.telecom.IInCallAdapter
        public void answerCall(String callId, int videoState) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void deflectCall(String callId, Uri address) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void rejectCall(String callId, boolean rejectWithMessage, String textMessage) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void rejectCallWithReason(String callId, int rejectReason) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void transferCall(String callId, Uri targetNumber, boolean isConfirmationRequired) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void consultativeTransfer(String callId, String otherCallId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void disconnectCall(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void holdCall(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void unholdCall(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void mute(boolean shouldMute) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void setAudioRoute(int route, String bluetoothAddress) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void enterBackgroundAudioProcessing(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void exitBackgroundAudioProcessing(String callId, boolean shouldRing) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void playDtmfTone(String callId, char digit) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void stopDtmfTone(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void postDialContinue(String callId, boolean proceed) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle, boolean setDefault) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void conference(String callId, String otherCallId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void splitFromConference(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void mergeConference(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void swapConference(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void addConferenceParticipants(String callId, List<Uri> participants) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void turnOnProximitySensor() throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void turnOffProximitySensor(boolean screenOnImmediately) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void pullExternalCall(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void sendCallEvent(String callId, String event, int targetSdkVer, Bundle extras) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void putExtras(String callId, Bundle extras) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void removeExtras(String callId, List<String> keys) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void sendRttRequest(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void respondToRttRequest(String callId, int id, boolean accept) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void stopRtt(String callId) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void setRttMode(String callId, int mode) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void handoverTo(String callId, PhoneAccountHandle destAcct, int videoState, Bundle extras) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallAdapter
        public void doTranAction(Bundle params) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IInCallAdapter {
        public static final String DESCRIPTOR = "com.android.internal.telecom.IInCallAdapter";
        static final int TRANSACTION_addConferenceParticipants = 22;
        static final int TRANSACTION_answerCall = 1;
        static final int TRANSACTION_conference = 18;
        static final int TRANSACTION_consultativeTransfer = 6;
        static final int TRANSACTION_deflectCall = 2;
        static final int TRANSACTION_disconnectCall = 7;
        static final int TRANSACTION_doTranAction = 34;
        static final int TRANSACTION_enterBackgroundAudioProcessing = 12;
        static final int TRANSACTION_exitBackgroundAudioProcessing = 13;
        static final int TRANSACTION_handoverTo = 33;
        static final int TRANSACTION_holdCall = 8;
        static final int TRANSACTION_mergeConference = 20;
        static final int TRANSACTION_mute = 10;
        static final int TRANSACTION_phoneAccountSelected = 17;
        static final int TRANSACTION_playDtmfTone = 14;
        static final int TRANSACTION_postDialContinue = 16;
        static final int TRANSACTION_pullExternalCall = 25;
        static final int TRANSACTION_putExtras = 27;
        static final int TRANSACTION_rejectCall = 3;
        static final int TRANSACTION_rejectCallWithReason = 4;
        static final int TRANSACTION_removeExtras = 28;
        static final int TRANSACTION_respondToRttRequest = 30;
        static final int TRANSACTION_sendCallEvent = 26;
        static final int TRANSACTION_sendRttRequest = 29;
        static final int TRANSACTION_setAudioRoute = 11;
        static final int TRANSACTION_setRttMode = 32;
        static final int TRANSACTION_splitFromConference = 19;
        static final int TRANSACTION_stopDtmfTone = 15;
        static final int TRANSACTION_stopRtt = 31;
        static final int TRANSACTION_swapConference = 21;
        static final int TRANSACTION_transferCall = 5;
        static final int TRANSACTION_turnOffProximitySensor = 24;
        static final int TRANSACTION_turnOnProximitySensor = 23;
        static final int TRANSACTION_unholdCall = 9;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInCallAdapter asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInCallAdapter)) {
                return (IInCallAdapter) iin;
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
                    return "answerCall";
                case 2:
                    return "deflectCall";
                case 3:
                    return "rejectCall";
                case 4:
                    return "rejectCallWithReason";
                case 5:
                    return "transferCall";
                case 6:
                    return "consultativeTransfer";
                case 7:
                    return "disconnectCall";
                case 8:
                    return "holdCall";
                case 9:
                    return "unholdCall";
                case 10:
                    return MediaMetrics.Value.MUTE;
                case 11:
                    return "setAudioRoute";
                case 12:
                    return "enterBackgroundAudioProcessing";
                case 13:
                    return "exitBackgroundAudioProcessing";
                case 14:
                    return "playDtmfTone";
                case 15:
                    return "stopDtmfTone";
                case 16:
                    return "postDialContinue";
                case 17:
                    return "phoneAccountSelected";
                case 18:
                    return ImsCallProfile.EXTRA_CONFERENCE_DEPRECATED;
                case 19:
                    return "splitFromConference";
                case 20:
                    return "mergeConference";
                case 21:
                    return "swapConference";
                case 22:
                    return "addConferenceParticipants";
                case 23:
                    return "turnOnProximitySensor";
                case 24:
                    return "turnOffProximitySensor";
                case 25:
                    return "pullExternalCall";
                case 26:
                    return "sendCallEvent";
                case 27:
                    return "putExtras";
                case 28:
                    return "removeExtras";
                case 29:
                    return "sendRttRequest";
                case 30:
                    return "respondToRttRequest";
                case 31:
                    return "stopRtt";
                case 32:
                    return "setRttMode";
                case 33:
                    return "handoverTo";
                case 34:
                    return "doTranAction";
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
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            answerCall(_arg0, _arg1);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            Uri _arg12 = (Uri) data.readTypedObject(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            deflectCall(_arg02, _arg12);
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            boolean _arg13 = data.readBoolean();
                            String _arg2 = data.readString();
                            data.enforceNoDataAvail();
                            rejectCall(_arg03, _arg13, _arg2);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            rejectCallWithReason(_arg04, _arg14);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            Uri _arg15 = (Uri) data.readTypedObject(Uri.CREATOR);
                            boolean _arg22 = data.readBoolean();
                            data.enforceNoDataAvail();
                            transferCall(_arg05, _arg15, _arg22);
                            break;
                        case 6:
                            String _arg06 = data.readString();
                            String _arg16 = data.readString();
                            data.enforceNoDataAvail();
                            consultativeTransfer(_arg06, _arg16);
                            break;
                        case 7:
                            String _arg07 = data.readString();
                            data.enforceNoDataAvail();
                            disconnectCall(_arg07);
                            break;
                        case 8:
                            String _arg08 = data.readString();
                            data.enforceNoDataAvail();
                            holdCall(_arg08);
                            break;
                        case 9:
                            String _arg09 = data.readString();
                            data.enforceNoDataAvail();
                            unholdCall(_arg09);
                            break;
                        case 10:
                            boolean _arg010 = data.readBoolean();
                            data.enforceNoDataAvail();
                            mute(_arg010);
                            break;
                        case 11:
                            int _arg011 = data.readInt();
                            String _arg17 = data.readString();
                            data.enforceNoDataAvail();
                            setAudioRoute(_arg011, _arg17);
                            break;
                        case 12:
                            String _arg012 = data.readString();
                            data.enforceNoDataAvail();
                            enterBackgroundAudioProcessing(_arg012);
                            break;
                        case 13:
                            String _arg013 = data.readString();
                            boolean _arg18 = data.readBoolean();
                            data.enforceNoDataAvail();
                            exitBackgroundAudioProcessing(_arg013, _arg18);
                            break;
                        case 14:
                            String _arg014 = data.readString();
                            char _arg19 = (char) data.readInt();
                            data.enforceNoDataAvail();
                            playDtmfTone(_arg014, _arg19);
                            break;
                        case 15:
                            String _arg015 = data.readString();
                            data.enforceNoDataAvail();
                            stopDtmfTone(_arg015);
                            break;
                        case 16:
                            String _arg016 = data.readString();
                            boolean _arg110 = data.readBoolean();
                            data.enforceNoDataAvail();
                            postDialContinue(_arg016, _arg110);
                            break;
                        case 17:
                            String _arg017 = data.readString();
                            PhoneAccountHandle _arg111 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
                            boolean _arg23 = data.readBoolean();
                            data.enforceNoDataAvail();
                            phoneAccountSelected(_arg017, _arg111, _arg23);
                            break;
                        case 18:
                            String _arg018 = data.readString();
                            String _arg112 = data.readString();
                            data.enforceNoDataAvail();
                            conference(_arg018, _arg112);
                            break;
                        case 19:
                            String _arg019 = data.readString();
                            data.enforceNoDataAvail();
                            splitFromConference(_arg019);
                            break;
                        case 20:
                            String _arg020 = data.readString();
                            data.enforceNoDataAvail();
                            mergeConference(_arg020);
                            break;
                        case 21:
                            String _arg021 = data.readString();
                            data.enforceNoDataAvail();
                            swapConference(_arg021);
                            break;
                        case 22:
                            String _arg022 = data.readString();
                            List<Uri> _arg113 = data.createTypedArrayList(Uri.CREATOR);
                            data.enforceNoDataAvail();
                            addConferenceParticipants(_arg022, _arg113);
                            break;
                        case 23:
                            turnOnProximitySensor();
                            break;
                        case 24:
                            boolean _arg023 = data.readBoolean();
                            data.enforceNoDataAvail();
                            turnOffProximitySensor(_arg023);
                            break;
                        case 25:
                            String _arg024 = data.readString();
                            data.enforceNoDataAvail();
                            pullExternalCall(_arg024);
                            break;
                        case 26:
                            String _arg025 = data.readString();
                            String _arg114 = data.readString();
                            int _arg24 = data.readInt();
                            Bundle _arg3 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            sendCallEvent(_arg025, _arg114, _arg24, _arg3);
                            break;
                        case 27:
                            String _arg026 = data.readString();
                            Bundle _arg115 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            putExtras(_arg026, _arg115);
                            break;
                        case 28:
                            String _arg027 = data.readString();
                            List<String> _arg116 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            removeExtras(_arg027, _arg116);
                            break;
                        case 29:
                            String _arg028 = data.readString();
                            data.enforceNoDataAvail();
                            sendRttRequest(_arg028);
                            break;
                        case 30:
                            String _arg029 = data.readString();
                            int _arg117 = data.readInt();
                            boolean _arg25 = data.readBoolean();
                            data.enforceNoDataAvail();
                            respondToRttRequest(_arg029, _arg117, _arg25);
                            break;
                        case 31:
                            String _arg030 = data.readString();
                            data.enforceNoDataAvail();
                            stopRtt(_arg030);
                            break;
                        case 32:
                            String _arg031 = data.readString();
                            int _arg118 = data.readInt();
                            data.enforceNoDataAvail();
                            setRttMode(_arg031, _arg118);
                            break;
                        case 33:
                            String _arg032 = data.readString();
                            PhoneAccountHandle _arg119 = (PhoneAccountHandle) data.readTypedObject(PhoneAccountHandle.CREATOR);
                            int _arg26 = data.readInt();
                            Bundle _arg32 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            handoverTo(_arg032, _arg119, _arg26, _arg32);
                            break;
                        case 34:
                            Bundle _arg033 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            doTranAction(_arg033);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IInCallAdapter {
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

            @Override // com.android.internal.telecom.IInCallAdapter
            public void answerCall(String callId, int videoState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(videoState);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void deflectCall(String callId, Uri address) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeTypedObject(address, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void rejectCall(String callId, boolean rejectWithMessage, String textMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeBoolean(rejectWithMessage);
                    _data.writeString(textMessage);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void rejectCallWithReason(String callId, int rejectReason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(rejectReason);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void transferCall(String callId, Uri targetNumber, boolean isConfirmationRequired) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeTypedObject(targetNumber, 0);
                    _data.writeBoolean(isConfirmationRequired);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void consultativeTransfer(String callId, String otherCallId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(otherCallId);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void disconnectCall(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void holdCall(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void unholdCall(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void mute(boolean shouldMute) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(shouldMute);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void setAudioRoute(int route, String bluetoothAddress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(route);
                    _data.writeString(bluetoothAddress);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void enterBackgroundAudioProcessing(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void exitBackgroundAudioProcessing(String callId, boolean shouldRing) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeBoolean(shouldRing);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void playDtmfTone(String callId, char digit) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(digit);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void stopDtmfTone(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void postDialContinue(String callId, boolean proceed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeBoolean(proceed);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle, boolean setDefault) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeTypedObject(accountHandle, 0);
                    _data.writeBoolean(setDefault);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void conference(String callId, String otherCallId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(otherCallId);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void splitFromConference(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void mergeConference(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void swapConference(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void addConferenceParticipants(String callId, List<Uri> participants) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeTypedList(participants);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void turnOnProximitySensor() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void turnOffProximitySensor(boolean screenOnImmediately) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(screenOnImmediately);
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void pullExternalCall(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void sendCallEvent(String callId, String event, int targetSdkVer, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(event);
                    _data.writeInt(targetSdkVer);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void putExtras(String callId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void removeExtras(String callId, List<String> keys) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeStringList(keys);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void sendRttRequest(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void respondToRttRequest(String callId, int id, boolean accept) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(id);
                    _data.writeBoolean(accept);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void stopRtt(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void setRttMode(String callId, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(mode);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void handoverTo(String callId, PhoneAccountHandle destAcct, int videoState, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeTypedObject(destAcct, 0);
                    _data.writeInt(videoState);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallAdapter
            public void doTranAction(Bundle params) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(params, 0);
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 33;
        }
    }
}
