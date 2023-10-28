package com.android.internal.view;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.view.InputChannel;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodSubtype;
import android.window.ImeOnBackInvokedDispatcher;
import com.android.internal.inputmethod.IInputMethodPrivilegedOperations;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import java.util.List;
/* loaded from: classes4.dex */
public interface IInputMethod extends IInterface {
    void bindInput(InputBinding inputBinding) throws RemoteException;

    void canStartStylusHandwriting(int i) throws RemoteException;

    void changeInputMethodSubtype(InputMethodSubtype inputMethodSubtype) throws RemoteException;

    void createSession(InputChannel inputChannel, IInputSessionCallback iInputSessionCallback) throws RemoteException;

    void finishStylusHandwriting() throws RemoteException;

    void hideSoftInput(IBinder iBinder, int i, ResultReceiver resultReceiver) throws RemoteException;

    void initInkWindow() throws RemoteException;

    void initializeInternal(IBinder iBinder, IInputMethodPrivilegedOperations iInputMethodPrivilegedOperations, int i, boolean z, int i2) throws RemoteException;

    void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo inlineSuggestionsRequestInfo, IInlineSuggestionsRequestCallback iInlineSuggestionsRequestCallback) throws RemoteException;

    void onNavButtonFlagsChanged(int i) throws RemoteException;

    void setSessionEnabled(IInputMethodSession iInputMethodSession, boolean z) throws RemoteException;

    void showSoftInput(IBinder iBinder, int i, ResultReceiver resultReceiver) throws RemoteException;

    void startInput(IBinder iBinder, IInputContext iInputContext, EditorInfo editorInfo, boolean z, int i, ImeOnBackInvokedDispatcher imeOnBackInvokedDispatcher) throws RemoteException;

    void startStylusHandwriting(int i, InputChannel inputChannel, List<MotionEvent> list) throws RemoteException;

    void unbindInput() throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IInputMethod {
        @Override // com.android.internal.view.IInputMethod
        public void initializeInternal(IBinder token, IInputMethodPrivilegedOperations privOps, int configChanges, boolean stylusHwSupported, int navigationBarFlags) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void bindInput(InputBinding binding) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void unbindInput() throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void startInput(IBinder startInputToken, IInputContext inputContext, EditorInfo attribute, boolean restarting, int navigationBarFlags, ImeOnBackInvokedDispatcher imeDispatcher) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void onNavButtonFlagsChanged(int navButtonFlags) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void createSession(InputChannel channel, IInputSessionCallback callback) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void setSessionEnabled(IInputMethodSession session, boolean enabled) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void showSoftInput(IBinder showInputToken, int flags, ResultReceiver resultReceiver) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void hideSoftInput(IBinder hideInputToken, int flags, ResultReceiver resultReceiver) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void changeInputMethodSubtype(InputMethodSubtype subtype) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void canStartStylusHandwriting(int requestId) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void startStylusHandwriting(int requestId, InputChannel channel, List<MotionEvent> events) throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void initInkWindow() throws RemoteException {
        }

        @Override // com.android.internal.view.IInputMethod
        public void finishStylusHandwriting() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IInputMethod {
        public static final String DESCRIPTOR = "com.android.internal.view.IInputMethod";
        static final int TRANSACTION_bindInput = 3;
        static final int TRANSACTION_canStartStylusHandwriting = 12;
        static final int TRANSACTION_changeInputMethodSubtype = 11;
        static final int TRANSACTION_createSession = 7;
        static final int TRANSACTION_finishStylusHandwriting = 15;
        static final int TRANSACTION_hideSoftInput = 10;
        static final int TRANSACTION_initInkWindow = 14;
        static final int TRANSACTION_initializeInternal = 1;
        static final int TRANSACTION_onCreateInlineSuggestionsRequest = 2;
        static final int TRANSACTION_onNavButtonFlagsChanged = 6;
        static final int TRANSACTION_setSessionEnabled = 8;
        static final int TRANSACTION_showSoftInput = 9;
        static final int TRANSACTION_startInput = 5;
        static final int TRANSACTION_startStylusHandwriting = 13;
        static final int TRANSACTION_unbindInput = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInputMethod asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IInputMethod)) {
                return (IInputMethod) iin;
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
                    return "initializeInternal";
                case 2:
                    return "onCreateInlineSuggestionsRequest";
                case 3:
                    return "bindInput";
                case 4:
                    return "unbindInput";
                case 5:
                    return "startInput";
                case 6:
                    return "onNavButtonFlagsChanged";
                case 7:
                    return "createSession";
                case 8:
                    return "setSessionEnabled";
                case 9:
                    return "showSoftInput";
                case 10:
                    return "hideSoftInput";
                case 11:
                    return "changeInputMethodSubtype";
                case 12:
                    return "canStartStylusHandwriting";
                case 13:
                    return "startStylusHandwriting";
                case 14:
                    return "initInkWindow";
                case 15:
                    return "finishStylusHandwriting";
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
                            IBinder _arg0 = data.readStrongBinder();
                            IInputMethodPrivilegedOperations _arg1 = IInputMethodPrivilegedOperations.Stub.asInterface(data.readStrongBinder());
                            int _arg2 = data.readInt();
                            boolean _arg3 = data.readBoolean();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            initializeInternal(_arg0, _arg1, _arg2, _arg3, _arg4);
                            break;
                        case 2:
                            InlineSuggestionsRequestInfo _arg02 = (InlineSuggestionsRequestInfo) data.readTypedObject(InlineSuggestionsRequestInfo.CREATOR);
                            IInlineSuggestionsRequestCallback _arg12 = IInlineSuggestionsRequestCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            onCreateInlineSuggestionsRequest(_arg02, _arg12);
                            break;
                        case 3:
                            InputBinding _arg03 = (InputBinding) data.readTypedObject(InputBinding.CREATOR);
                            data.enforceNoDataAvail();
                            bindInput(_arg03);
                            break;
                        case 4:
                            unbindInput();
                            break;
                        case 5:
                            IBinder _arg04 = data.readStrongBinder();
                            IInputContext _arg13 = IInputContext.Stub.asInterface(data.readStrongBinder());
                            EditorInfo _arg22 = (EditorInfo) data.readTypedObject(EditorInfo.CREATOR);
                            boolean _arg32 = data.readBoolean();
                            int _arg42 = data.readInt();
                            ImeOnBackInvokedDispatcher _arg5 = (ImeOnBackInvokedDispatcher) data.readTypedObject(ImeOnBackInvokedDispatcher.CREATOR);
                            data.enforceNoDataAvail();
                            startInput(_arg04, _arg13, _arg22, _arg32, _arg42, _arg5);
                            break;
                        case 6:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            onNavButtonFlagsChanged(_arg05);
                            break;
                        case 7:
                            InputChannel _arg06 = (InputChannel) data.readTypedObject(InputChannel.CREATOR);
                            IInputSessionCallback _arg14 = IInputSessionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            createSession(_arg06, _arg14);
                            break;
                        case 8:
                            IBinder _arg07 = data.readStrongBinder();
                            IInputMethodSession _arg08 = IInputMethodSession.Stub.asInterface(_arg07);
                            boolean _arg15 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setSessionEnabled(_arg08, _arg15);
                            break;
                        case 9:
                            IBinder _arg09 = data.readStrongBinder();
                            int _arg16 = data.readInt();
                            ResultReceiver _arg23 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            data.enforceNoDataAvail();
                            showSoftInput(_arg09, _arg16, _arg23);
                            break;
                        case 10:
                            IBinder _arg010 = data.readStrongBinder();
                            int _arg17 = data.readInt();
                            ResultReceiver _arg24 = (ResultReceiver) data.readTypedObject(ResultReceiver.CREATOR);
                            data.enforceNoDataAvail();
                            hideSoftInput(_arg010, _arg17, _arg24);
                            break;
                        case 11:
                            InputMethodSubtype _arg011 = (InputMethodSubtype) data.readTypedObject(InputMethodSubtype.CREATOR);
                            data.enforceNoDataAvail();
                            changeInputMethodSubtype(_arg011);
                            break;
                        case 12:
                            int _arg012 = data.readInt();
                            data.enforceNoDataAvail();
                            canStartStylusHandwriting(_arg012);
                            break;
                        case 13:
                            int _arg013 = data.readInt();
                            InputChannel _arg18 = (InputChannel) data.readTypedObject(InputChannel.CREATOR);
                            List<MotionEvent> _arg25 = data.createTypedArrayList(MotionEvent.CREATOR);
                            data.enforceNoDataAvail();
                            startStylusHandwriting(_arg013, _arg18, _arg25);
                            break;
                        case 14:
                            initInkWindow();
                            break;
                        case 15:
                            finishStylusHandwriting();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes4.dex */
        private static class Proxy implements IInputMethod {
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

            @Override // com.android.internal.view.IInputMethod
            public void initializeInternal(IBinder token, IInputMethodPrivilegedOperations privOps, int configChanges, boolean stylusHwSupported, int navigationBarFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    _data.writeStrongInterface(privOps);
                    _data.writeInt(configChanges);
                    _data.writeBoolean(stylusHwSupported);
                    _data.writeInt(navigationBarFlags);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void onCreateInlineSuggestionsRequest(InlineSuggestionsRequestInfo requestInfo, IInlineSuggestionsRequestCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(requestInfo, 0);
                    _data.writeStrongInterface(cb);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void bindInput(InputBinding binding) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(binding, 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void unbindInput() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void startInput(IBinder startInputToken, IInputContext inputContext, EditorInfo attribute, boolean restarting, int navigationBarFlags, ImeOnBackInvokedDispatcher imeDispatcher) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(startInputToken);
                    _data.writeStrongInterface(inputContext);
                    _data.writeTypedObject(attribute, 0);
                    _data.writeBoolean(restarting);
                    _data.writeInt(navigationBarFlags);
                    _data.writeTypedObject(imeDispatcher, 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void onNavButtonFlagsChanged(int navButtonFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(navButtonFlags);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void createSession(InputChannel channel, IInputSessionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(channel, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void setSessionEnabled(IInputMethodSession session, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(session);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void showSoftInput(IBinder showInputToken, int flags, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(showInputToken);
                    _data.writeInt(flags);
                    _data.writeTypedObject(resultReceiver, 0);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void hideSoftInput(IBinder hideInputToken, int flags, ResultReceiver resultReceiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(hideInputToken);
                    _data.writeInt(flags);
                    _data.writeTypedObject(resultReceiver, 0);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void changeInputMethodSubtype(InputMethodSubtype subtype) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(subtype, 0);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void canStartStylusHandwriting(int requestId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(requestId);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void startStylusHandwriting(int requestId, InputChannel channel, List<MotionEvent> events) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(requestId);
                    _data.writeTypedObject(channel, 0);
                    _data.writeTypedList(events);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void initInkWindow() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.view.IInputMethod
            public void finishStylusHandwriting() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 14;
        }
    }
}
