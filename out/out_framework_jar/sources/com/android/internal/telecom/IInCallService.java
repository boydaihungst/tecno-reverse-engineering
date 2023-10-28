package com.android.internal.telecom;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.CallAudioState;
import android.telecom.ParcelableCall;
import com.android.internal.telecom.IInCallAdapter;
/* loaded from: classes4.dex */
public interface IInCallService extends IInterface {
    void addCall(ParcelableCall parcelableCall) throws RemoteException;

    void bringToForeground(boolean z) throws RemoteException;

    void onCallAudioStateChanged(CallAudioState callAudioState) throws RemoteException;

    void onCanAddCallChanged(boolean z) throws RemoteException;

    void onConnectionEvent(String str, String str2, Bundle bundle) throws RemoteException;

    void onHandoverComplete(String str) throws RemoteException;

    void onHandoverFailed(String str, int i) throws RemoteException;

    void onRttInitiationFailure(String str, int i) throws RemoteException;

    void onRttUpgradeRequest(String str, int i) throws RemoteException;

    void setInCallAdapter(IInCallAdapter iInCallAdapter) throws RemoteException;

    void setPostDial(String str, String str2) throws RemoteException;

    void setPostDialWait(String str, String str2) throws RemoteException;

    void silenceRinger() throws RemoteException;

    void updateCall(ParcelableCall parcelableCall) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IInCallService {
        @Override // com.android.internal.telecom.IInCallService
        public void setInCallAdapter(IInCallAdapter inCallAdapter) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void addCall(ParcelableCall call) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void updateCall(ParcelableCall call) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void setPostDial(String callId, String remaining) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void setPostDialWait(String callId, String remaining) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onCallAudioStateChanged(CallAudioState callAudioState) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void bringToForeground(boolean showDialpad) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onCanAddCallChanged(boolean canAddCall) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void silenceRinger() throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onConnectionEvent(String callId, String event, Bundle extras) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onRttUpgradeRequest(String callId, int id) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onRttInitiationFailure(String callId, int reason) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onHandoverFailed(String callId, int error) throws RemoteException {
        }

        @Override // com.android.internal.telecom.IInCallService
        public void onHandoverComplete(String callId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IInCallService {
        public static final String DESCRIPTOR = "com.android.internal.telecom.IInCallService";
        static final int TRANSACTION_addCall = 2;
        static final int TRANSACTION_bringToForeground = 7;
        static final int TRANSACTION_onCallAudioStateChanged = 6;
        static final int TRANSACTION_onCanAddCallChanged = 8;
        static final int TRANSACTION_onConnectionEvent = 10;
        static final int TRANSACTION_onHandoverComplete = 14;
        static final int TRANSACTION_onHandoverFailed = 13;
        static final int TRANSACTION_onRttInitiationFailure = 12;
        static final int TRANSACTION_onRttUpgradeRequest = 11;
        static final int TRANSACTION_setInCallAdapter = 1;
        static final int TRANSACTION_setPostDial = 4;
        static final int TRANSACTION_setPostDialWait = 5;
        static final int TRANSACTION_silenceRinger = 9;
        static final int TRANSACTION_updateCall = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInCallService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInCallService)) {
                return (IInCallService) iin;
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
                    return "setInCallAdapter";
                case 2:
                    return "addCall";
                case 3:
                    return "updateCall";
                case 4:
                    return "setPostDial";
                case 5:
                    return "setPostDialWait";
                case 6:
                    return "onCallAudioStateChanged";
                case 7:
                    return "bringToForeground";
                case 8:
                    return "onCanAddCallChanged";
                case 9:
                    return "silenceRinger";
                case 10:
                    return "onConnectionEvent";
                case 11:
                    return "onRttUpgradeRequest";
                case 12:
                    return "onRttInitiationFailure";
                case 13:
                    return "onHandoverFailed";
                case 14:
                    return "onHandoverComplete";
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
                            IInCallAdapter _arg0 = IInCallAdapter.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            setInCallAdapter(_arg0);
                            break;
                        case 2:
                            ParcelableCall _arg02 = (ParcelableCall) data.readTypedObject(ParcelableCall.CREATOR);
                            data.enforceNoDataAvail();
                            addCall(_arg02);
                            break;
                        case 3:
                            ParcelableCall _arg03 = (ParcelableCall) data.readTypedObject(ParcelableCall.CREATOR);
                            data.enforceNoDataAvail();
                            updateCall(_arg03);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            String _arg1 = data.readString();
                            data.enforceNoDataAvail();
                            setPostDial(_arg04, _arg1);
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            String _arg12 = data.readString();
                            data.enforceNoDataAvail();
                            setPostDialWait(_arg05, _arg12);
                            break;
                        case 6:
                            CallAudioState _arg06 = (CallAudioState) data.readTypedObject(CallAudioState.CREATOR);
                            data.enforceNoDataAvail();
                            onCallAudioStateChanged(_arg06);
                            break;
                        case 7:
                            boolean _arg07 = data.readBoolean();
                            data.enforceNoDataAvail();
                            bringToForeground(_arg07);
                            break;
                        case 8:
                            boolean _arg08 = data.readBoolean();
                            data.enforceNoDataAvail();
                            onCanAddCallChanged(_arg08);
                            break;
                        case 9:
                            silenceRinger();
                            break;
                        case 10:
                            String _arg09 = data.readString();
                            String _arg13 = data.readString();
                            Bundle _arg2 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            onConnectionEvent(_arg09, _arg13, _arg2);
                            break;
                        case 11:
                            String _arg010 = data.readString();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            onRttUpgradeRequest(_arg010, _arg14);
                            break;
                        case 12:
                            String _arg011 = data.readString();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            onRttInitiationFailure(_arg011, _arg15);
                            break;
                        case 13:
                            String _arg012 = data.readString();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            onHandoverFailed(_arg012, _arg16);
                            break;
                        case 14:
                            String _arg013 = data.readString();
                            data.enforceNoDataAvail();
                            onHandoverComplete(_arg013);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IInCallService {
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

            @Override // com.android.internal.telecom.IInCallService
            public void setInCallAdapter(IInCallAdapter inCallAdapter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(inCallAdapter);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void addCall(ParcelableCall call) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(call, 0);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void updateCall(ParcelableCall call) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(call, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void setPostDial(String callId, String remaining) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(remaining);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void setPostDialWait(String callId, String remaining) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(remaining);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onCallAudioStateChanged(CallAudioState callAudioState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(callAudioState, 0);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void bringToForeground(boolean showDialpad) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(showDialpad);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onCanAddCallChanged(boolean canAddCall) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(canAddCall);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void silenceRinger() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onConnectionEvent(String callId, String event, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeString(event);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onRttUpgradeRequest(String callId, int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(id);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onRttInitiationFailure(String callId, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(reason);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onHandoverFailed(String callId, int error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    _data.writeInt(error);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.telecom.IInCallService
            public void onHandoverComplete(String callId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callId);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 13;
        }
    }
}
