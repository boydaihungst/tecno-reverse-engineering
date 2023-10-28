package com.android.internal.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.inputmethod.IRemoteAccessibilityInputConnection;
import com.android.internal.inputmethod.InputBindResult;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import java.util.List;
/* loaded from: classes4.dex */
public interface IInputMethodManager extends IInterface {
    void addClient(IInputMethodClient iInputMethodClient, IInputContext iInputContext, int i) throws RemoteException;

    void clearClient(IInputMethodClient iInputMethodClient) throws RemoteException;

    void commitConnectKeyAndText(KeyEvent keyEvent, String str) throws RemoteException;

    List<InputMethodInfo> getAwareLockedInputMethodList(int i, int i2) throws RemoteException;

    InputMethodSubtype getCurrentInputMethodSubtype() throws RemoteException;

    List<InputMethodInfo> getEnabledInputMethodList(int i) throws RemoteException;

    List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String str, boolean z) throws RemoteException;

    List<InputMethodInfo> getInputMethodList(int i) throws RemoteException;

    int getInputMethodWindowVisibleHeight(IInputMethodClient iInputMethodClient) throws RemoteException;

    InputMethodSubtype getLastInputMethodSubtype() throws RemoteException;

    boolean hideSoftInput(IInputMethodClient iInputMethodClient, IBinder iBinder, int i, ResultReceiver resultReceiver, int i2) throws RemoteException;

    boolean isImeTraceEnabled() throws RemoteException;

    boolean isInputMethodPickerShownForTest() throws RemoteException;

    void removeImeSurface() throws RemoteException;

    void removeImeSurfaceFromWindowAsync(IBinder iBinder) throws RemoteException;

    void reportPerceptibleAsync(IBinder iBinder, boolean z) throws RemoteException;

    void reportVirtualDisplayGeometryAsync(IInputMethodClient iInputMethodClient, int i, float[] fArr) throws RemoteException;

    void setAdditionalInputMethodSubtypes(String str, InputMethodSubtype[] inputMethodSubtypeArr) throws RemoteException;

    void setConnectSessionId(int i) throws RemoteException;

    void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient iInputMethodClient, String str) throws RemoteException;

    void showInputMethodPickerFromClient(IInputMethodClient iInputMethodClient, int i) throws RemoteException;

    void showInputMethodPickerFromSystem(IInputMethodClient iInputMethodClient, int i, int i2) throws RemoteException;

    boolean showSoftInput(IInputMethodClient iInputMethodClient, IBinder iBinder, int i, ResultReceiver resultReceiver, int i2) throws RemoteException;

    void startImeTrace() throws RemoteException;

    InputBindResult startInputOrWindowGainedFocus(int i, IInputMethodClient iInputMethodClient, IBinder iBinder, int i2, int i3, int i4, EditorInfo editorInfo, IInputContext iInputContext, IRemoteAccessibilityInputConnection iRemoteAccessibilityInputConnection, int i5, ImeOnBackInvokedDispatcher imeOnBackInvokedDispatcher) throws RemoteException;

    void startProtoDump(byte[] bArr, int i, String str) throws RemoteException;

    void startStylusHandwriting(IInputMethodClient iInputMethodClient) throws RemoteException;

    void stopImeTrace() throws RemoteException;

    void updateSecurityInputBlackList(List<String> list) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IInputMethodManager {
        @Override // com.android.internal.view.IInputMethodManager
        public void addClient(IInputMethodClient client, IInputContext inputContext, int untrustedDisplayId) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public List<InputMethodInfo> getInputMethodList(int userId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public List<InputMethodInfo> getAwareLockedInputMethodList(int userId, int directBootAwareness) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public List<InputMethodInfo> getEnabledInputMethodList(int userId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId, boolean allowsImplicitlySelectedSubtypes) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public InputMethodSubtype getLastInputMethodSubtype() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public boolean showSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public boolean hideSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int startInputFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, int unverifiedTargetSdkVersion, ImeOnBackInvokedDispatcher imeDispatcher) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void showInputMethodPickerFromSystem(IInputMethodClient client, int auxiliarySubtypeMode, int displayId) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String topId) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public boolean isInputMethodPickerShownForTest() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public InputMethodSubtype getCurrentInputMethodSubtype() throws RemoteException {
            return null;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void setAdditionalInputMethodSubtypes(String id, InputMethodSubtype[] subtypes) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public int getInputMethodWindowVisibleHeight(IInputMethodClient client) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void reportVirtualDisplayGeometryAsync(IInputMethodClient parentClient, int childDisplayId, float[] matrixValues) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void reportPerceptibleAsync(IBinder windowToken, boolean perceptible) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void removeImeSurface() throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void removeImeSurfaceFromWindowAsync(IBinder windowToken) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void startProtoDump(byte[] protoDump, int source, String where) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public boolean isImeTraceEnabled() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void startImeTrace() throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void stopImeTrace() throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void startStylusHandwriting(IInputMethodClient client) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void clearClient(IInputMethodClient client) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void updateSecurityInputBlackList(List<String> blacklist) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void commitConnectKeyAndText(KeyEvent keyEvent, String text) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethodManager
        public void setConnectSessionId(int sessionId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IInputMethodManager {
        public static final String DESCRIPTOR = "com.android.internal.view.IInputMethodManager";
        static final int TRANSACTION_addClient = 1;
        static final int TRANSACTION_clearClient = 26;
        static final int TRANSACTION_commitConnectKeyAndText = 28;
        static final int TRANSACTION_getAwareLockedInputMethodList = 3;
        static final int TRANSACTION_getCurrentInputMethodSubtype = 14;
        static final int TRANSACTION_getEnabledInputMethodList = 4;
        static final int TRANSACTION_getEnabledInputMethodSubtypeList = 5;
        static final int TRANSACTION_getInputMethodList = 2;
        static final int TRANSACTION_getInputMethodWindowVisibleHeight = 16;
        static final int TRANSACTION_getLastInputMethodSubtype = 6;
        static final int TRANSACTION_hideSoftInput = 8;
        static final int TRANSACTION_isImeTraceEnabled = 22;
        static final int TRANSACTION_isInputMethodPickerShownForTest = 13;
        static final int TRANSACTION_removeImeSurface = 19;
        static final int TRANSACTION_removeImeSurfaceFromWindowAsync = 20;
        static final int TRANSACTION_reportPerceptibleAsync = 18;
        static final int TRANSACTION_reportVirtualDisplayGeometryAsync = 17;
        static final int TRANSACTION_setAdditionalInputMethodSubtypes = 15;
        static final int TRANSACTION_setConnectSessionId = 29;
        static final int TRANSACTION_showInputMethodAndSubtypeEnablerFromClient = 12;
        static final int TRANSACTION_showInputMethodPickerFromClient = 10;
        static final int TRANSACTION_showInputMethodPickerFromSystem = 11;
        static final int TRANSACTION_showSoftInput = 7;
        static final int TRANSACTION_startImeTrace = 23;
        static final int TRANSACTION_startInputOrWindowGainedFocus = 9;
        static final int TRANSACTION_startProtoDump = 21;
        static final int TRANSACTION_startStylusHandwriting = 25;
        static final int TRANSACTION_stopImeTrace = 24;
        static final int TRANSACTION_updateSecurityInputBlackList = 27;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInputMethodManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInputMethodManager)) {
                return (IInputMethodManager) iin;
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
                    return "addClient";
                case 2:
                    return "getInputMethodList";
                case 3:
                    return "getAwareLockedInputMethodList";
                case 4:
                    return "getEnabledInputMethodList";
                case 5:
                    return "getEnabledInputMethodSubtypeList";
                case 6:
                    return "getLastInputMethodSubtype";
                case 7:
                    return "showSoftInput";
                case 8:
                    return "hideSoftInput";
                case 9:
                    return "startInputOrWindowGainedFocus";
                case 10:
                    return "showInputMethodPickerFromClient";
                case 11:
                    return "showInputMethodPickerFromSystem";
                case 12:
                    return "showInputMethodAndSubtypeEnablerFromClient";
                case 13:
                    return "isInputMethodPickerShownForTest";
                case 14:
                    return "getCurrentInputMethodSubtype";
                case 15:
                    return "setAdditionalInputMethodSubtypes";
                case 16:
                    return "getInputMethodWindowVisibleHeight";
                case 17:
                    return "reportVirtualDisplayGeometryAsync";
                case 18:
                    return "reportPerceptibleAsync";
                case 19:
                    return "removeImeSurface";
                case 20:
                    return "removeImeSurfaceFromWindowAsync";
                case 21:
                    return "startProtoDump";
                case 22:
                    return "isImeTraceEnabled";
                case 23:
                    return "startImeTrace";
                case 24:
                    return "stopImeTrace";
                case 25:
                    return "startStylusHandwriting";
                case 26:
                    return "clearClient";
                case 27:
                    return "updateSecurityInputBlackList";
                case 28:
                    return "commitConnectKeyAndText";
                case 29:
                    return "setConnectSessionId";
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
                            IInputMethodClient _arg0 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            IInputContext _arg1 = IInputContext.Stub.asInterface(data.readStrongBinder());
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            addClient(_arg0, _arg1, _arg2);
                            reply.writeNoException();
                            return true;
                        case 2:
                            int _arg02 = data.readInt();
                            data.enforceNoDataAvail();
                            List<InputMethodInfo> _result = getInputMethodList(_arg02);
                            reply.writeNoException();
                            reply.writeTypedList(_result);
                            return true;
                        case 3:
                            int _arg03 = data.readInt();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            List<InputMethodInfo> _result2 = getAwareLockedInputMethodList(_arg03, _arg12);
                            reply.writeNoException();
                            reply.writeTypedList(_result2);
                            return true;
                        case 4:
                            int _arg04 = data.readInt();
                            data.enforceNoDataAvail();
                            List<InputMethodInfo> _result3 = getEnabledInputMethodList(_arg04);
                            reply.writeNoException();
                            reply.writeTypedList(_result3);
                            return true;
                        case 5:
                            String _arg05 = data.readString();
                            boolean _arg13 = data.readBoolean();
                            data.enforceNoDataAvail();
                            List<InputMethodSubtype> _result4 = getEnabledInputMethodSubtypeList(_arg05, _arg13);
                            reply.writeNoException();
                            reply.writeTypedList(_result4);
                            return true;
                        case 6:
                            InputMethodSubtype _result5 = getLastInputMethodSubtype();
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            return true;
                        case 7:
                            IInputMethodClient _arg06 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg14 = data.readStrongBinder();
                            int _arg22 = data.readInt();
                            ResultReceiver _arg3 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result6 = showSoftInput(_arg06, _arg14, _arg22, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            return true;
                        case 8:
                            IInputMethodClient _arg07 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg15 = data.readStrongBinder();
                            int _arg23 = data.readInt();
                            ResultReceiver _arg32 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            int _arg42 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result7 = hideSoftInput(_arg07, _arg15, _arg23, _arg32, _arg42);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            return true;
                        case 9:
                            int _arg08 = data.readInt();
                            IInputMethodClient _arg16 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg24 = data.readStrongBinder();
                            int _arg33 = data.readInt();
                            int _arg43 = data.readInt();
                            int _arg5 = data.readInt();
                            EditorInfo _arg6 = (EditorInfo) data.readTypedObject(EditorInfo.CREATOR);
                            IInputContext _arg7 = IInputContext.Stub.asInterface(data.readStrongBinder());
                            IRemoteAccessibilityInputConnection _arg8 = IRemoteAccessibilityInputConnection.Stub.asInterface(data.readStrongBinder());
                            int _arg9 = data.readInt();
                            ImeOnBackInvokedDispatcher _arg10 = (ImeOnBackInvokedDispatcher) data.readTypedObject(ImeOnBackInvokedDispatcher.CREATOR);
                            data.enforceNoDataAvail();
                            InputBindResult _result8 = startInputOrWindowGainedFocus(_arg08, _arg16, _arg24, _arg33, _arg43, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10);
                            reply.writeNoException();
                            reply.writeTypedObject(_result8, 1);
                            return true;
                        case 10:
                            IInputMethodClient _arg09 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            showInputMethodPickerFromClient(_arg09, _arg17);
                            reply.writeNoException();
                            return true;
                        case 11:
                            IInputMethodClient _arg010 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            int _arg18 = data.readInt();
                            int _arg25 = data.readInt();
                            data.enforceNoDataAvail();
                            showInputMethodPickerFromSystem(_arg010, _arg18, _arg25);
                            reply.writeNoException();
                            return true;
                        case 12:
                            IInputMethodClient _arg011 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            String _arg19 = data.readString();
                            data.enforceNoDataAvail();
                            showInputMethodAndSubtypeEnablerFromClient(_arg011, _arg19);
                            reply.writeNoException();
                            return true;
                        case 13:
                            boolean _result9 = isInputMethodPickerShownForTest();
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            return true;
                        case 14:
                            InputMethodSubtype _result10 = getCurrentInputMethodSubtype();
                            reply.writeNoException();
                            reply.writeTypedObject(_result10, 1);
                            return true;
                        case 15:
                            String _arg012 = data.readString();
                            InputMethodSubtype[] _arg110 = (InputMethodSubtype[]) data.createTypedArray(InputMethodSubtype.CREATOR);
                            data.enforceNoDataAvail();
                            setAdditionalInputMethodSubtypes(_arg012, _arg110);
                            reply.writeNoException();
                            return true;
                        case 16:
                            IInputMethodClient _arg013 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            int _result11 = getInputMethodWindowVisibleHeight(_arg013);
                            reply.writeNoException();
                            reply.writeInt(_result11);
                            return true;
                        case 17:
                            IBinder _arg014 = data.readStrongBinder();
                            IInputMethodClient _arg015 = IInputMethodClient.Stub.asInterface(_arg014);
                            int _arg111 = data.readInt();
                            float[] _arg26 = data.createFloatArray();
                            data.enforceNoDataAvail();
                            reportVirtualDisplayGeometryAsync(_arg015, _arg111, _arg26);
                            return true;
                        case 18:
                            IBinder _arg016 = data.readStrongBinder();
                            boolean _arg112 = data.readBoolean();
                            data.enforceNoDataAvail();
                            reportPerceptibleAsync(_arg016, _arg112);
                            return true;
                        case 19:
                            removeImeSurface();
                            reply.writeNoException();
                            return true;
                        case 20:
                            IBinder _arg017 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            removeImeSurfaceFromWindowAsync(_arg017);
                            return true;
                        case 21:
                            byte[] _arg018 = data.createByteArray();
                            int _arg113 = data.readInt();
                            String _arg27 = data.readString();
                            data.enforceNoDataAvail();
                            startProtoDump(_arg018, _arg113, _arg27);
                            reply.writeNoException();
                            return true;
                        case 22:
                            boolean _result12 = isImeTraceEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            return true;
                        case 23:
                            startImeTrace();
                            reply.writeNoException();
                            return true;
                        case 24:
                            stopImeTrace();
                            reply.writeNoException();
                            return true;
                        case 25:
                            IInputMethodClient _arg019 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            startStylusHandwriting(_arg019);
                            reply.writeNoException();
                            return true;
                        case 26:
                            IInputMethodClient _arg020 = IInputMethodClient.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            clearClient(_arg020);
                            reply.writeNoException();
                            return true;
                        case 27:
                            List<String> _arg021 = data.createStringArrayList();
                            data.enforceNoDataAvail();
                            updateSecurityInputBlackList(_arg021);
                            reply.writeNoException();
                            return true;
                        case 28:
                            KeyEvent _arg022 = (KeyEvent) data.readTypedObject(KeyEvent.CREATOR);
                            String _arg114 = data.readString();
                            data.enforceNoDataAvail();
                            commitConnectKeyAndText(_arg022, _arg114);
                            reply.writeNoException();
                            return true;
                        case 29:
                            int _arg023 = data.readInt();
                            data.enforceNoDataAvail();
                            setConnectSessionId(_arg023);
                            reply.writeNoException();
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements IInputMethodManager {
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

            @Override // com.android.internal.view.IInputMethodManager
            public void addClient(IInputMethodClient client, IInputContext inputContext, int untrustedDisplayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeStrongInterface(inputContext);
                    _data.writeInt(untrustedDisplayId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public List<InputMethodInfo> getInputMethodList(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodInfo> _result = _reply.createTypedArrayList(InputMethodInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public List<InputMethodInfo> getAwareLockedInputMethodList(int userId, int directBootAwareness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    _data.writeInt(directBootAwareness);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodInfo> _result = _reply.createTypedArrayList(InputMethodInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public List<InputMethodInfo> getEnabledInputMethodList(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodInfo> _result = _reply.createTypedArrayList(InputMethodInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId, boolean allowsImplicitlySelectedSubtypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(imiId);
                    _data.writeBoolean(allowsImplicitlySelectedSubtypes);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<InputMethodSubtype> _result = _reply.createTypedArrayList(InputMethodSubtype.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public InputMethodSubtype getLastInputMethodSubtype() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    InputMethodSubtype _result = (InputMethodSubtype) _reply.readTypedObject(InputMethodSubtype.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public boolean showSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeStrongBinder(windowToken);
                    _data.writeInt(flags);
                    _data.writeTypedObject(resultReceiver, 0);
                    _data.writeInt(reason);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public boolean hideSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeStrongBinder(windowToken);
                    _data.writeInt(flags);
                    _data.writeTypedObject(resultReceiver, 0);
                    _data.writeInt(reason);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int startInputFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, IRemoteAccessibilityInputConnection remoteAccessibilityInputConnection, int unverifiedTargetSdkVersion, ImeOnBackInvokedDispatcher imeDispatcher) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startInputReason);
                    try {
                        _data.writeStrongInterface(client);
                        try {
                            _data.writeStrongBinder(windowToken);
                            try {
                                _data.writeInt(startInputFlags);
                            } catch (Throwable th) {
                                th = th;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(softInputMode);
                        try {
                            _data.writeInt(windowFlags);
                            try {
                                _data.writeTypedObject(attribute, 0);
                                try {
                                    _data.writeStrongInterface(inputContext);
                                } catch (Throwable th4) {
                                    th = th4;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeStrongInterface(remoteAccessibilityInputConnection);
                            try {
                                _data.writeInt(unverifiedTargetSdkVersion);
                                try {
                                    _data.writeTypedObject(imeDispatcher, 0);
                                    try {
                                        this.mRemote.transact(9, _data, _reply, 0);
                                        _reply.readException();
                                        InputBindResult _result = (InputBindResult) _reply.readTypedObject(InputBindResult.CREATOR);
                                        _reply.recycle();
                                        _data.recycle();
                                        return _result;
                                    } catch (Throwable th7) {
                                        th = th7;
                                        _reply.recycle();
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeInt(auxiliarySubtypeMode);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void showInputMethodPickerFromSystem(IInputMethodClient client, int auxiliarySubtypeMode, int displayId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeInt(auxiliarySubtypeMode);
                    _data.writeInt(displayId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String topId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeString(topId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public boolean isInputMethodPickerShownForTest() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public InputMethodSubtype getCurrentInputMethodSubtype() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    InputMethodSubtype _result = (InputMethodSubtype) _reply.readTypedObject(InputMethodSubtype.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void setAdditionalInputMethodSubtypes(String id, InputMethodSubtype[] subtypes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(id);
                    _data.writeTypedArray(subtypes, 0);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public int getInputMethodWindowVisibleHeight(IInputMethodClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void reportVirtualDisplayGeometryAsync(IInputMethodClient parentClient, int childDisplayId, float[] matrixValues) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(parentClient);
                    _data.writeInt(childDisplayId);
                    _data.writeFloatArray(matrixValues);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void reportPerceptibleAsync(IBinder windowToken, boolean perceptible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    _data.writeBoolean(perceptible);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void removeImeSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void removeImeSurfaceFromWindowAsync(IBinder windowToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void startProtoDump(byte[] protoDump, int source, String where) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(protoDump);
                    _data.writeInt(source);
                    _data.writeString(where);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public boolean isImeTraceEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void startImeTrace() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void stopImeTrace() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void startStylusHandwriting(IInputMethodClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void clearClient(IInputMethodClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void updateSecurityInputBlackList(List<String> blacklist) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringList(blacklist);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void commitConnectKeyAndText(KeyEvent keyEvent, String text) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(keyEvent, 0);
                    _data.writeString(text);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethodManager
            public void setConnectSessionId(int sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
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
