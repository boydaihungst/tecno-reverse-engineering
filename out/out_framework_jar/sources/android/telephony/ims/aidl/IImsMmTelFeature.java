package android.telephony.ims.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.RtpHeaderExtensionType;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsMmTelListener;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import java.util.List;
/* loaded from: classes3.dex */
public interface IImsMmTelFeature extends IInterface {
    public static final String DESCRIPTOR = "android.telephony.ims.aidl.IImsMmTelFeature";

    void acknowledgeSms(int i, int i2, int i3) throws RemoteException;

    void acknowledgeSmsReport(int i, int i2, int i3) throws RemoteException;

    void addCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void changeCapabilitiesConfiguration(CapabilityChangeRequest capabilityChangeRequest, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void changeOfferedRtpHeaderExtensionTypes(List<RtpHeaderExtensionType> list) throws RemoteException;

    ImsCallProfile createCallProfile(int i, int i2) throws RemoteException;

    IImsCallSession createCallSession(ImsCallProfile imsCallProfile) throws RemoteException;

    IImsEcbm getEcbmInterface() throws RemoteException;

    int getFeatureState() throws RemoteException;

    IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException;

    String getSmsFormat() throws RemoteException;

    IImsUt getUtInterface() throws RemoteException;

    void onSmsReady() throws RemoteException;

    void queryCapabilityConfiguration(int i, int i2, IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    int queryCapabilityStatus() throws RemoteException;

    void removeCapabilityCallback(IImsCapabilityCallback iImsCapabilityCallback) throws RemoteException;

    void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) throws RemoteException;

    void setListener(IImsMmTelListener iImsMmTelListener) throws RemoteException;

    void setSmsListener(IImsSmsListener iImsSmsListener) throws RemoteException;

    void setUiTtyMode(int i, Message message) throws RemoteException;

    int shouldProcessCall(String[] strArr) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IImsMmTelFeature {
        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void setListener(IImsMmTelListener l) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public int getFeatureState() throws RemoteException {
            return 0;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public ImsCallProfile createCallProfile(int callSessionType, int callType) throws RemoteException {
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void changeOfferedRtpHeaderExtensionTypes(List<RtpHeaderExtensionType> types) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsCallSession createCallSession(ImsCallProfile profile) throws RemoteException {
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public int shouldProcessCall(String[] uris) throws RemoteException {
            return 0;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsUt getUtInterface() throws RemoteException {
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsEcbm getEcbmInterface() throws RemoteException {
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void setUiTtyMode(int uiTtyMode, Message onCompleteMessage) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public int queryCapabilityStatus() throws RemoteException {
            return 0;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void addCapabilityCallback(IImsCapabilityCallback c) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void removeCapabilityCallback(IImsCapabilityCallback c) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void changeCapabilitiesConfiguration(CapabilityChangeRequest request, IImsCapabilityCallback c) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void queryCapabilityConfiguration(int capability, int radioTech, IImsCapabilityCallback c) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void setSmsListener(IImsSmsListener l) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void sendSms(int token, int messageRef, String format, String smsc, boolean retry, byte[] pdu) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void acknowledgeSms(int token, int messageRef, int result) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void acknowledgeSmsReport(int token, int messageRef, int result) throws RemoteException {
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public String getSmsFormat() throws RemoteException {
            return null;
        }

        @Override // android.telephony.ims.aidl.IImsMmTelFeature
        public void onSmsReady() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IImsMmTelFeature {
        static final int TRANSACTION_acknowledgeSms = 18;
        static final int TRANSACTION_acknowledgeSmsReport = 19;
        static final int TRANSACTION_addCapabilityCallback = 12;
        static final int TRANSACTION_changeCapabilitiesConfiguration = 14;
        static final int TRANSACTION_changeOfferedRtpHeaderExtensionTypes = 4;
        static final int TRANSACTION_createCallProfile = 3;
        static final int TRANSACTION_createCallSession = 5;
        static final int TRANSACTION_getEcbmInterface = 8;
        static final int TRANSACTION_getFeatureState = 2;
        static final int TRANSACTION_getMultiEndpointInterface = 10;
        static final int TRANSACTION_getSmsFormat = 20;
        static final int TRANSACTION_getUtInterface = 7;
        static final int TRANSACTION_onSmsReady = 21;
        static final int TRANSACTION_queryCapabilityConfiguration = 15;
        static final int TRANSACTION_queryCapabilityStatus = 11;
        static final int TRANSACTION_removeCapabilityCallback = 13;
        static final int TRANSACTION_sendSms = 17;
        static final int TRANSACTION_setListener = 1;
        static final int TRANSACTION_setSmsListener = 16;
        static final int TRANSACTION_setUiTtyMode = 9;
        static final int TRANSACTION_shouldProcessCall = 6;

        public Stub() {
            attachInterface(this, IImsMmTelFeature.DESCRIPTOR);
        }

        public static IImsMmTelFeature asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IImsMmTelFeature.DESCRIPTOR);
            if (iin != null && (iin instanceof IImsMmTelFeature)) {
                return (IImsMmTelFeature) iin;
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
                    return "setListener";
                case 2:
                    return "getFeatureState";
                case 3:
                    return "createCallProfile";
                case 4:
                    return "changeOfferedRtpHeaderExtensionTypes";
                case 5:
                    return "createCallSession";
                case 6:
                    return "shouldProcessCall";
                case 7:
                    return "getUtInterface";
                case 8:
                    return "getEcbmInterface";
                case 9:
                    return "setUiTtyMode";
                case 10:
                    return "getMultiEndpointInterface";
                case 11:
                    return "queryCapabilityStatus";
                case 12:
                    return "addCapabilityCallback";
                case 13:
                    return "removeCapabilityCallback";
                case 14:
                    return "changeCapabilitiesConfiguration";
                case 15:
                    return "queryCapabilityConfiguration";
                case 16:
                    return "setSmsListener";
                case 17:
                    return "sendSms";
                case 18:
                    return "acknowledgeSms";
                case 19:
                    return "acknowledgeSmsReport";
                case 20:
                    return "getSmsFormat";
                case 21:
                    return "onSmsReady";
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
                data.enforceInterface(IImsMmTelFeature.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IImsMmTelFeature.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            IImsMmTelListener _arg0 = IImsMmTelListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setListener(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            int _result = getFeatureState();
                            reply.writeNoException();
                            reply.writeInt(_result);
                            break;
                        case 3:
                            int _arg02 = data.readInt();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            ImsCallProfile _result2 = createCallProfile(_arg02, _arg1);
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 4:
                            List<RtpHeaderExtensionType> _arg03 = data.createTypedArrayList(RtpHeaderExtensionType.CREATOR);
                            data.enforceNoDataAvail();
                            changeOfferedRtpHeaderExtensionTypes(_arg03);
                            reply.writeNoException();
                            break;
                        case 5:
                            ImsCallProfile _arg04 = (ImsCallProfile) data.readTypedObject(ImsCallProfile.CREATOR);
                            data.enforceNoDataAvail();
                            IImsCallSession _result3 = createCallSession(_arg04);
                            reply.writeNoException();
                            reply.writeStrongInterface(_result3);
                            break;
                        case 6:
                            String[] _arg05 = data.createStringArray();
                            data.enforceNoDataAvail();
                            int _result4 = shouldProcessCall(_arg05);
                            reply.writeNoException();
                            reply.writeInt(_result4);
                            break;
                        case 7:
                            IImsUt _result5 = getUtInterface();
                            reply.writeNoException();
                            reply.writeStrongInterface(_result5);
                            break;
                        case 8:
                            IImsEcbm _result6 = getEcbmInterface();
                            reply.writeNoException();
                            reply.writeStrongInterface(_result6);
                            break;
                        case 9:
                            int _arg06 = data.readInt();
                            Message _arg12 = (Message) data.readTypedObject(Message.CREATOR);
                            data.enforceNoDataAvail();
                            setUiTtyMode(_arg06, _arg12);
                            reply.writeNoException();
                            break;
                        case 10:
                            IImsMultiEndpoint _result7 = getMultiEndpointInterface();
                            reply.writeNoException();
                            reply.writeStrongInterface(_result7);
                            break;
                        case 11:
                            int _result8 = queryCapabilityStatus();
                            reply.writeNoException();
                            reply.writeInt(_result8);
                            break;
                        case 12:
                            IImsCapabilityCallback _arg07 = IImsCapabilityCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            addCapabilityCallback(_arg07);
                            break;
                        case 13:
                            IImsCapabilityCallback _arg08 = IImsCapabilityCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeCapabilityCallback(_arg08);
                            break;
                        case 14:
                            CapabilityChangeRequest _arg09 = (CapabilityChangeRequest) data.readTypedObject(CapabilityChangeRequest.CREATOR);
                            IImsCapabilityCallback _arg13 = IImsCapabilityCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            changeCapabilitiesConfiguration(_arg09, _arg13);
                            break;
                        case 15:
                            int _arg010 = data.readInt();
                            int _arg14 = data.readInt();
                            IImsCapabilityCallback _arg2 = IImsCapabilityCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            queryCapabilityConfiguration(_arg010, _arg14, _arg2);
                            break;
                        case 16:
                            IImsSmsListener _arg011 = IImsSmsListener.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setSmsListener(_arg011);
                            reply.writeNoException();
                            break;
                        case 17:
                            int _arg012 = data.readInt();
                            int _arg15 = data.readInt();
                            String _arg22 = data.readString();
                            String _arg3 = data.readString();
                            boolean _arg4 = data.readBoolean();
                            byte[] _arg5 = data.createByteArray();
                            data.enforceNoDataAvail();
                            sendSms(_arg012, _arg15, _arg22, _arg3, _arg4, _arg5);
                            break;
                        case 18:
                            int _arg013 = data.readInt();
                            int _arg16 = data.readInt();
                            int _arg23 = data.readInt();
                            data.enforceNoDataAvail();
                            acknowledgeSms(_arg013, _arg16, _arg23);
                            break;
                        case 19:
                            int _arg014 = data.readInt();
                            int _arg17 = data.readInt();
                            int _arg24 = data.readInt();
                            data.enforceNoDataAvail();
                            acknowledgeSmsReport(_arg014, _arg17, _arg24);
                            break;
                        case 20:
                            String _result9 = getSmsFormat();
                            reply.writeNoException();
                            reply.writeString(_result9);
                            break;
                        case 21:
                            onSmsReady();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IImsMmTelFeature {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IImsMmTelFeature.DESCRIPTOR;
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void setListener(IImsMmTelListener l) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeStrongInterface(l);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public int getFeatureState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public ImsCallProfile createCallProfile(int callSessionType, int callType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeInt(callSessionType);
                    _data.writeInt(callType);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    ImsCallProfile _result = (ImsCallProfile) _reply.readTypedObject(ImsCallProfile.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void changeOfferedRtpHeaderExtensionTypes(List<RtpHeaderExtensionType> types) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeTypedList(types);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public IImsCallSession createCallSession(ImsCallProfile profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeTypedObject(profile, 0);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    IImsCallSession _result = IImsCallSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public int shouldProcessCall(String[] uris) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeStringArray(uris);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public IImsUt getUtInterface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    IImsUt _result = IImsUt.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public IImsEcbm getEcbmInterface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    IImsEcbm _result = IImsEcbm.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void setUiTtyMode(int uiTtyMode, Message onCompleteMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeInt(uiTtyMode);
                    _data.writeTypedObject(onCompleteMessage, 0);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    IImsMultiEndpoint _result = IImsMultiEndpoint.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public int queryCapabilityStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void addCapabilityCallback(IImsCapabilityCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void removeCapabilityCallback(IImsCapabilityCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void changeCapabilitiesConfiguration(CapabilityChangeRequest request, IImsCapabilityCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeTypedObject(request, 0);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void queryCapabilityConfiguration(int capability, int radioTech, IImsCapabilityCallback c) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeInt(capability);
                    _data.writeInt(radioTech);
                    _data.writeStrongInterface(c);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void setSmsListener(IImsSmsListener l) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeStrongInterface(l);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void sendSms(int token, int messageRef, String format, String smsc, boolean retry, byte[] pdu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeInt(messageRef);
                    _data.writeString(format);
                    _data.writeString(smsc);
                    _data.writeBoolean(retry);
                    _data.writeByteArray(pdu);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void acknowledgeSms(int token, int messageRef, int result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeInt(messageRef);
                    _data.writeInt(result);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void acknowledgeSmsReport(int token, int messageRef, int result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    _data.writeInt(token);
                    _data.writeInt(messageRef);
                    _data.writeInt(result);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public String getSmsFormat() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.telephony.ims.aidl.IImsMmTelFeature
            public void onSmsReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImsMmTelFeature.DESCRIPTOR);
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
