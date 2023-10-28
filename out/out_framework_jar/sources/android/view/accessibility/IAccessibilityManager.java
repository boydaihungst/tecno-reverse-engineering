package android.view.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.app.RemoteAction;
import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IWindow;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityManagerClient;
import android.view.accessibility.IWindowMagnificationConnection;
import java.util.List;
/* loaded from: classes3.dex */
public interface IAccessibilityManager extends IInterface {
    int addAccessibilityInteractionConnection(IWindow iWindow, IBinder iBinder, IAccessibilityInteractionConnection iAccessibilityInteractionConnection, String str, int i) throws RemoteException;

    long addClient(IAccessibilityManagerClient iAccessibilityManagerClient, int i) throws RemoteException;

    void associateEmbeddedHierarchy(IBinder iBinder, IBinder iBinder2) throws RemoteException;

    void disassociateEmbeddedHierarchy(IBinder iBinder) throws RemoteException;

    List<String> getAccessibilityShortcutTargets(int i) throws RemoteException;

    int getAccessibilityWindowId(IBinder iBinder) throws RemoteException;

    List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int i, int i2) throws RemoteException;

    int getFocusColor() throws RemoteException;

    int getFocusStrokeWidth() throws RemoteException;

    List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int i) throws RemoteException;

    long getRecommendedTimeoutMillis() throws RemoteException;

    IBinder getWindowToken(int i, int i2) throws RemoteException;

    void interrupt(int i) throws RemoteException;

    boolean isAudioDescriptionByDefaultEnabled() throws RemoteException;

    boolean isSystemAudioCaptioningUiEnabled(int i) throws RemoteException;

    void notifyAccessibilityButtonClicked(int i, String str) throws RemoteException;

    void notifyAccessibilityButtonVisibilityChanged(boolean z) throws RemoteException;

    void performAccessibilityShortcut(String str) throws RemoteException;

    void registerSystemAction(RemoteAction remoteAction, int i) throws RemoteException;

    void registerUiTestAutomationService(IBinder iBinder, IAccessibilityServiceClient iAccessibilityServiceClient, AccessibilityServiceInfo accessibilityServiceInfo, int i) throws RemoteException;

    void removeAccessibilityInteractionConnection(IWindow iWindow) throws RemoteException;

    boolean removeClient(IAccessibilityManagerClient iAccessibilityManagerClient, int i) throws RemoteException;

    void sendAccessibilityEvent(AccessibilityEvent accessibilityEvent, int i) throws RemoteException;

    boolean sendFingerprintGesture(int i) throws RemoteException;

    void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection iAccessibilityInteractionConnection) throws RemoteException;

    void setSystemAudioCaptioningEnabled(boolean z, int i) throws RemoteException;

    void setSystemAudioCaptioningUiEnabled(boolean z, int i) throws RemoteException;

    void setWindowMagnificationConnection(IWindowMagnificationConnection iWindowMagnificationConnection) throws RemoteException;

    void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName componentName, boolean z) throws RemoteException;

    void unregisterSystemAction(int i) throws RemoteException;

    void unregisterUiTestAutomationService(IAccessibilityServiceClient iAccessibilityServiceClient) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IAccessibilityManager {
        @Override // android.view.accessibility.IAccessibilityManager
        public void interrupt(int userId) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void sendAccessibilityEvent(AccessibilityEvent uiEvent, int userId) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public long addClient(IAccessibilityManagerClient client, int userId) throws RemoteException {
            return 0L;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public boolean removeClient(IAccessibilityManagerClient client, int userId) throws RemoteException {
            return false;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int userId) throws RemoteException {
            return null;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) throws RemoteException {
            return null;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public int addAccessibilityInteractionConnection(IWindow windowToken, IBinder leashToken, IAccessibilityInteractionConnection connection, String packageName, int userId) throws RemoteException {
            return 0;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void removeAccessibilityInteractionConnection(IWindow windowToken) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection connection) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void registerUiTestAutomationService(IBinder owner, IAccessibilityServiceClient client, AccessibilityServiceInfo info, int flags) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void unregisterUiTestAutomationService(IAccessibilityServiceClient client) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName service, boolean touchExplorationEnabled) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public IBinder getWindowToken(int windowId, int userId) throws RemoteException {
            return null;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void notifyAccessibilityButtonClicked(int displayId, String targetName) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void notifyAccessibilityButtonVisibilityChanged(boolean available) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void performAccessibilityShortcut(String targetName) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public List<String> getAccessibilityShortcutTargets(int shortcutType) throws RemoteException {
            return null;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public boolean sendFingerprintGesture(int gestureKeyCode) throws RemoteException {
            return false;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public int getAccessibilityWindowId(IBinder windowToken) throws RemoteException {
            return 0;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public long getRecommendedTimeoutMillis() throws RemoteException {
            return 0L;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void registerSystemAction(RemoteAction action, int actionId) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void unregisterSystemAction(int actionId) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void setWindowMagnificationConnection(IWindowMagnificationConnection connection) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void associateEmbeddedHierarchy(IBinder host, IBinder embedded) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void disassociateEmbeddedHierarchy(IBinder token) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public int getFocusStrokeWidth() throws RemoteException {
            return 0;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public int getFocusColor() throws RemoteException {
            return 0;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public boolean isAudioDescriptionByDefaultEnabled() throws RemoteException {
            return false;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void setSystemAudioCaptioningEnabled(boolean isEnabled, int userId) throws RemoteException {
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public boolean isSystemAudioCaptioningUiEnabled(int userId) throws RemoteException {
            return false;
        }

        @Override // android.view.accessibility.IAccessibilityManager
        public void setSystemAudioCaptioningUiEnabled(boolean isEnabled, int userId) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IAccessibilityManager {
        public static final String DESCRIPTOR = "android.view.accessibility.IAccessibilityManager";
        static final int TRANSACTION_addAccessibilityInteractionConnection = 7;
        static final int TRANSACTION_addClient = 3;
        static final int TRANSACTION_associateEmbeddedHierarchy = 24;
        static final int TRANSACTION_disassociateEmbeddedHierarchy = 25;
        static final int TRANSACTION_getAccessibilityShortcutTargets = 17;
        static final int TRANSACTION_getAccessibilityWindowId = 19;
        static final int TRANSACTION_getEnabledAccessibilityServiceList = 6;
        static final int TRANSACTION_getFocusColor = 27;
        static final int TRANSACTION_getFocusStrokeWidth = 26;
        static final int TRANSACTION_getInstalledAccessibilityServiceList = 5;
        static final int TRANSACTION_getRecommendedTimeoutMillis = 20;
        static final int TRANSACTION_getWindowToken = 13;
        static final int TRANSACTION_interrupt = 1;
        static final int TRANSACTION_isAudioDescriptionByDefaultEnabled = 28;
        static final int TRANSACTION_isSystemAudioCaptioningUiEnabled = 30;
        static final int TRANSACTION_notifyAccessibilityButtonClicked = 14;
        static final int TRANSACTION_notifyAccessibilityButtonVisibilityChanged = 15;
        static final int TRANSACTION_performAccessibilityShortcut = 16;
        static final int TRANSACTION_registerSystemAction = 21;
        static final int TRANSACTION_registerUiTestAutomationService = 10;
        static final int TRANSACTION_removeAccessibilityInteractionConnection = 8;
        static final int TRANSACTION_removeClient = 4;
        static final int TRANSACTION_sendAccessibilityEvent = 2;
        static final int TRANSACTION_sendFingerprintGesture = 18;
        static final int TRANSACTION_setPictureInPictureActionReplacingConnection = 9;
        static final int TRANSACTION_setSystemAudioCaptioningEnabled = 29;
        static final int TRANSACTION_setSystemAudioCaptioningUiEnabled = 31;
        static final int TRANSACTION_setWindowMagnificationConnection = 23;
        static final int TRANSACTION_temporaryEnableAccessibilityStateUntilKeyguardRemoved = 12;
        static final int TRANSACTION_unregisterSystemAction = 22;
        static final int TRANSACTION_unregisterUiTestAutomationService = 11;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccessibilityManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAccessibilityManager)) {
                return (IAccessibilityManager) iin;
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
                    return "interrupt";
                case 2:
                    return "sendAccessibilityEvent";
                case 3:
                    return "addClient";
                case 4:
                    return "removeClient";
                case 5:
                    return "getInstalledAccessibilityServiceList";
                case 6:
                    return "getEnabledAccessibilityServiceList";
                case 7:
                    return "addAccessibilityInteractionConnection";
                case 8:
                    return "removeAccessibilityInteractionConnection";
                case 9:
                    return "setPictureInPictureActionReplacingConnection";
                case 10:
                    return "registerUiTestAutomationService";
                case 11:
                    return "unregisterUiTestAutomationService";
                case 12:
                    return "temporaryEnableAccessibilityStateUntilKeyguardRemoved";
                case 13:
                    return "getWindowToken";
                case 14:
                    return "notifyAccessibilityButtonClicked";
                case 15:
                    return "notifyAccessibilityButtonVisibilityChanged";
                case 16:
                    return "performAccessibilityShortcut";
                case 17:
                    return "getAccessibilityShortcutTargets";
                case 18:
                    return "sendFingerprintGesture";
                case 19:
                    return "getAccessibilityWindowId";
                case 20:
                    return "getRecommendedTimeoutMillis";
                case 21:
                    return "registerSystemAction";
                case 22:
                    return "unregisterSystemAction";
                case 23:
                    return "setWindowMagnificationConnection";
                case 24:
                    return "associateEmbeddedHierarchy";
                case 25:
                    return "disassociateEmbeddedHierarchy";
                case 26:
                    return "getFocusStrokeWidth";
                case 27:
                    return "getFocusColor";
                case 28:
                    return "isAudioDescriptionByDefaultEnabled";
                case 29:
                    return "setSystemAudioCaptioningEnabled";
                case 30:
                    return "isSystemAudioCaptioningUiEnabled";
                case 31:
                    return "setSystemAudioCaptioningUiEnabled";
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
                            int _arg0 = data.readInt();
                            data.enforceNoDataAvail();
                            interrupt(_arg0);
                            break;
                        case 2:
                            AccessibilityEvent _arg02 = (AccessibilityEvent) data.readTypedObject(AccessibilityEvent.CREATOR);
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            sendAccessibilityEvent(_arg02, _arg1);
                            break;
                        case 3:
                            IAccessibilityManagerClient _arg03 = IAccessibilityManagerClient.Stub.asInterface(data.readStrongBinder());
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            long _result = addClient(_arg03, _arg12);
                            reply.writeNoException();
                            reply.writeLong(_result);
                            break;
                        case 4:
                            IAccessibilityManagerClient _arg04 = IAccessibilityManagerClient.Stub.asInterface(data.readStrongBinder());
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result2 = removeClient(_arg04, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            data.enforceNoDataAvail();
                            List<AccessibilityServiceInfo> _result3 = getInstalledAccessibilityServiceList(_arg05);
                            reply.writeNoException();
                            reply.writeTypedList(_result3);
                            break;
                        case 6:
                            int _arg06 = data.readInt();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            List<AccessibilityServiceInfo> _result4 = getEnabledAccessibilityServiceList(_arg06, _arg14);
                            reply.writeNoException();
                            reply.writeTypedList(_result4);
                            break;
                        case 7:
                            IWindow _arg07 = IWindow.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg15 = data.readStrongBinder();
                            IAccessibilityInteractionConnection _arg2 = IAccessibilityInteractionConnection.Stub.asInterface(data.readStrongBinder());
                            String _arg3 = data.readString();
                            int _arg4 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result5 = addAccessibilityInteractionConnection(_arg07, _arg15, _arg2, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeInt(_result5);
                            break;
                        case 8:
                            IWindow _arg08 = IWindow.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            removeAccessibilityInteractionConnection(_arg08);
                            reply.writeNoException();
                            break;
                        case 9:
                            IBinder _arg09 = data.readStrongBinder();
                            IAccessibilityInteractionConnection _arg010 = IAccessibilityInteractionConnection.Stub.asInterface(_arg09);
                            data.enforceNoDataAvail();
                            setPictureInPictureActionReplacingConnection(_arg010);
                            reply.writeNoException();
                            break;
                        case 10:
                            IBinder _arg011 = data.readStrongBinder();
                            IAccessibilityServiceClient _arg16 = IAccessibilityServiceClient.Stub.asInterface(data.readStrongBinder());
                            AccessibilityServiceInfo _arg22 = (AccessibilityServiceInfo) data.readTypedObject(AccessibilityServiceInfo.CREATOR);
                            int _arg32 = data.readInt();
                            data.enforceNoDataAvail();
                            registerUiTestAutomationService(_arg011, _arg16, _arg22, _arg32);
                            reply.writeNoException();
                            break;
                        case 11:
                            IAccessibilityServiceClient _arg012 = IAccessibilityServiceClient.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            unregisterUiTestAutomationService(_arg012);
                            reply.writeNoException();
                            break;
                        case 12:
                            ComponentName _arg013 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            boolean _arg17 = data.readBoolean();
                            data.enforceNoDataAvail();
                            temporaryEnableAccessibilityStateUntilKeyguardRemoved(_arg013, _arg17);
                            reply.writeNoException();
                            break;
                        case 13:
                            int _arg014 = data.readInt();
                            int _arg18 = data.readInt();
                            data.enforceNoDataAvail();
                            IBinder _result6 = getWindowToken(_arg014, _arg18);
                            reply.writeNoException();
                            reply.writeStrongBinder(_result6);
                            break;
                        case 14:
                            int _arg015 = data.readInt();
                            String _arg19 = data.readString();
                            data.enforceNoDataAvail();
                            notifyAccessibilityButtonClicked(_arg015, _arg19);
                            reply.writeNoException();
                            break;
                        case 15:
                            boolean _arg016 = data.readBoolean();
                            data.enforceNoDataAvail();
                            notifyAccessibilityButtonVisibilityChanged(_arg016);
                            reply.writeNoException();
                            break;
                        case 16:
                            String _arg017 = data.readString();
                            data.enforceNoDataAvail();
                            performAccessibilityShortcut(_arg017);
                            reply.writeNoException();
                            break;
                        case 17:
                            int _arg018 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result7 = getAccessibilityShortcutTargets(_arg018);
                            reply.writeNoException();
                            reply.writeStringList(_result7);
                            break;
                        case 18:
                            int _arg019 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result8 = sendFingerprintGesture(_arg019);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 19:
                            IBinder _arg020 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            int _result9 = getAccessibilityWindowId(_arg020);
                            reply.writeNoException();
                            reply.writeInt(_result9);
                            break;
                        case 20:
                            long _result10 = getRecommendedTimeoutMillis();
                            reply.writeNoException();
                            reply.writeLong(_result10);
                            break;
                        case 21:
                            RemoteAction _arg021 = (RemoteAction) data.readTypedObject(RemoteAction.CREATOR);
                            int _arg110 = data.readInt();
                            data.enforceNoDataAvail();
                            registerSystemAction(_arg021, _arg110);
                            break;
                        case 22:
                            int _arg022 = data.readInt();
                            data.enforceNoDataAvail();
                            unregisterSystemAction(_arg022);
                            break;
                        case 23:
                            IBinder _arg023 = data.readStrongBinder();
                            IWindowMagnificationConnection _arg024 = IWindowMagnificationConnection.Stub.asInterface(_arg023);
                            data.enforceNoDataAvail();
                            setWindowMagnificationConnection(_arg024);
                            break;
                        case 24:
                            IBinder _arg025 = data.readStrongBinder();
                            IBinder _arg111 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            associateEmbeddedHierarchy(_arg025, _arg111);
                            reply.writeNoException();
                            break;
                        case 25:
                            IBinder _arg026 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            disassociateEmbeddedHierarchy(_arg026);
                            reply.writeNoException();
                            break;
                        case 26:
                            int _result11 = getFocusStrokeWidth();
                            reply.writeNoException();
                            reply.writeInt(_result11);
                            break;
                        case 27:
                            int _result12 = getFocusColor();
                            reply.writeNoException();
                            reply.writeInt(_result12);
                            break;
                        case 28:
                            boolean _result13 = isAudioDescriptionByDefaultEnabled();
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case 29:
                            boolean _arg027 = data.readBoolean();
                            int _arg112 = data.readInt();
                            data.enforceNoDataAvail();
                            setSystemAudioCaptioningEnabled(_arg027, _arg112);
                            reply.writeNoException();
                            break;
                        case 30:
                            int _arg028 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result14 = isSystemAudioCaptioningUiEnabled(_arg028);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case 31:
                            boolean _arg029 = data.readBoolean();
                            int _arg113 = data.readInt();
                            data.enforceNoDataAvail();
                            setSystemAudioCaptioningUiEnabled(_arg029, _arg113);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes3.dex */
        public static class Proxy implements IAccessibilityManager {
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

            @Override // android.view.accessibility.IAccessibilityManager
            public void interrupt(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void sendAccessibilityEvent(AccessibilityEvent uiEvent, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(uiEvent, 0);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public long addClient(IAccessibilityManagerClient client, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeInt(userId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public boolean removeClient(IAccessibilityManagerClient client, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<AccessibilityServiceInfo> _result = _reply.createTypedArrayList(AccessibilityServiceInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(feedbackType);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<AccessibilityServiceInfo> _result = _reply.createTypedArrayList(AccessibilityServiceInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public int addAccessibilityInteractionConnection(IWindow windowToken, IBinder leashToken, IAccessibilityInteractionConnection connection, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(windowToken);
                    _data.writeStrongBinder(leashToken);
                    _data.writeStrongInterface(connection);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void removeAccessibilityInteractionConnection(IWindow windowToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(windowToken);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection connection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(connection);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void registerUiTestAutomationService(IBinder owner, IAccessibilityServiceClient client, AccessibilityServiceInfo info, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(owner);
                    _data.writeStrongInterface(client);
                    _data.writeTypedObject(info, 0);
                    _data.writeInt(flags);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void unregisterUiTestAutomationService(IAccessibilityServiceClient client) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(client);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName service, boolean touchExplorationEnabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(service, 0);
                    _data.writeBoolean(touchExplorationEnabled);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public IBinder getWindowToken(int windowId, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(windowId);
                    _data.writeInt(userId);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void notifyAccessibilityButtonClicked(int displayId, String targetName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(displayId);
                    _data.writeString(targetName);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void notifyAccessibilityButtonVisibilityChanged(boolean available) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(available);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void performAccessibilityShortcut(String targetName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(targetName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public List<String> getAccessibilityShortcutTargets(int shortcutType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(shortcutType);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public boolean sendFingerprintGesture(int gestureKeyCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(gestureKeyCode);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public int getAccessibilityWindowId(IBinder windowToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(windowToken);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public long getRecommendedTimeoutMillis() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void registerSystemAction(RemoteAction action, int actionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(action, 0);
                    _data.writeInt(actionId);
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void unregisterSystemAction(int actionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(actionId);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void setWindowMagnificationConnection(IWindowMagnificationConnection connection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(connection);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void associateEmbeddedHierarchy(IBinder host, IBinder embedded) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(host);
                    _data.writeStrongBinder(embedded);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void disassociateEmbeddedHierarchy(IBinder token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public int getFocusStrokeWidth() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public int getFocusColor() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public boolean isAudioDescriptionByDefaultEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void setSystemAudioCaptioningEnabled(boolean isEnabled, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isEnabled);
                    _data.writeInt(userId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public boolean isSystemAudioCaptioningUiEnabled(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.view.accessibility.IAccessibilityManager
            public void setSystemAudioCaptioningUiEnabled(boolean isEnabled, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBoolean(isEnabled);
                    _data.writeInt(userId);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 30;
        }
    }
}
