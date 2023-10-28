package com.android.internal.appwidget;

import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.RemoteViews;
import com.android.internal.appwidget.IAppWidgetHost;
import java.util.List;
/* loaded from: classes4.dex */
public interface IAppWidgetService extends IInterface {
    int allocateAppWidgetId(String str, int i) throws RemoteException;

    boolean bindAppWidgetId(String str, int i, int i2, ComponentName componentName, Bundle bundle) throws RemoteException;

    boolean bindRemoteViewsService(String str, int i, Intent intent, IApplicationThread iApplicationThread, IBinder iBinder, IServiceConnection iServiceConnection, int i2) throws RemoteException;

    IntentSender createAppWidgetConfigIntentSender(String str, int i, int i2) throws RemoteException;

    void deleteAllHosts() throws RemoteException;

    void deleteAppWidgetId(String str, int i) throws RemoteException;

    void deleteHost(String str, int i) throws RemoteException;

    int[] getAppWidgetIds(ComponentName componentName) throws RemoteException;

    int[] getAppWidgetIdsForHost(String str, int i) throws RemoteException;

    AppWidgetProviderInfo getAppWidgetInfo(String str, int i) throws RemoteException;

    List<ComponentName> getAppWidgetOfHost(String str, int i) throws RemoteException;

    Bundle getAppWidgetOptions(String str, int i) throws RemoteException;

    RemoteViews getAppWidgetViews(String str, int i) throws RemoteException;

    List<String> getBoundAppWidgets(String str) throws RemoteException;

    ParceledListSlice getInstalledProvidersForProfile(int i, int i2, String str) throws RemoteException;

    boolean hasBindAppWidgetPermission(String str, int i) throws RemoteException;

    boolean isBoundWidgetPackage(String str, int i) throws RemoteException;

    boolean isRequestPinAppWidgetSupported() throws RemoteException;

    void noteAppWidgetTapped(String str, int i) throws RemoteException;

    void notifyAppWidgetViewDataChanged(String str, int[] iArr, int i) throws RemoteException;

    void partiallyUpdateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews) throws RemoteException;

    boolean requestPinAppWidget(String str, ComponentName componentName, Bundle bundle, IntentSender intentSender) throws RemoteException;

    void setBindAppWidgetPermission(String str, int i, boolean z) throws RemoteException;

    ParceledListSlice startListening(IAppWidgetHost iAppWidgetHost, String str, int i, int[] iArr) throws RemoteException;

    void stopListening(String str, int i) throws RemoteException;

    void updateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews) throws RemoteException;

    void updateAppWidgetOptions(String str, int i, Bundle bundle) throws RemoteException;

    void updateAppWidgetProvider(ComponentName componentName, RemoteViews remoteViews) throws RemoteException;

    void updateAppWidgetProviderInfo(ComponentName componentName, String str) throws RemoteException;

    /* loaded from: classes4.dex */
    public static class Default implements IAppWidgetService {
        @Override // com.android.internal.appwidget.IAppWidgetService
        public ParceledListSlice startListening(IAppWidgetHost host, String callingPackage, int hostId, int[] appWidgetIds) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void stopListening(String callingPackage, int hostId) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public int allocateAppWidgetId(String callingPackage, int hostId) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void deleteAppWidgetId(String callingPackage, int appWidgetId) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void deleteHost(String packageName, int hostId) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void deleteAllHosts() throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public int[] getAppWidgetIdsForHost(String callingPackage, int hostId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId, int intentFlags) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle extras) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public Bundle getAppWidgetOptions(String callingPackage, int appWidgetId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void updateAppWidgetProvider(ComponentName provider, RemoteViews views) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void updateAppWidgetProviderInfo(ComponentName provider, String metadataKey) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void notifyAppWidgetViewDataChanged(String packageName, int[] appWidgetIds, int viewId) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public ParceledListSlice getInstalledProvidersForProfile(int categoryFilter, int profileId, String packageName) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public boolean hasBindAppWidgetPermission(String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void setBindAppWidgetPermission(String packageName, int userId, boolean permission) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public boolean bindAppWidgetId(String callingPackage, int appWidgetId, int providerProfileId, ComponentName providerComponent, Bundle options) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public boolean bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent, IApplicationThread caller, IBinder token, IServiceConnection connection, int flags) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public int[] getAppWidgetIds(ComponentName providerComponent) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public boolean isBoundWidgetPackage(String packageName, int userId) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public boolean requestPinAppWidget(String packageName, ComponentName providerComponent, Bundle extras, IntentSender resultIntent) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public boolean isRequestPinAppWidgetSupported() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public void noteAppWidgetTapped(String callingPackage, int appWidgetId) throws RemoteException {
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public List<ComponentName> getAppWidgetOfHost(String pkg, int uid) throws RemoteException {
            return null;
        }

        @Override // com.android.internal.appwidget.IAppWidgetService
        public List<String> getBoundAppWidgets(String host) throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends Binder implements IAppWidgetService {
        public static final String DESCRIPTOR = "com.android.internal.appwidget.IAppWidgetService";
        static final int TRANSACTION_allocateAppWidgetId = 3;
        static final int TRANSACTION_bindAppWidgetId = 21;
        static final int TRANSACTION_bindRemoteViewsService = 22;
        static final int TRANSACTION_createAppWidgetConfigIntentSender = 9;
        static final int TRANSACTION_deleteAllHosts = 6;
        static final int TRANSACTION_deleteAppWidgetId = 4;
        static final int TRANSACTION_deleteHost = 5;
        static final int TRANSACTION_getAppWidgetIds = 23;
        static final int TRANSACTION_getAppWidgetIdsForHost = 8;
        static final int TRANSACTION_getAppWidgetInfo = 18;
        static final int TRANSACTION_getAppWidgetOfHost = 28;
        static final int TRANSACTION_getAppWidgetOptions = 12;
        static final int TRANSACTION_getAppWidgetViews = 7;
        static final int TRANSACTION_getBoundAppWidgets = 29;
        static final int TRANSACTION_getInstalledProvidersForProfile = 17;
        static final int TRANSACTION_hasBindAppWidgetPermission = 19;
        static final int TRANSACTION_isBoundWidgetPackage = 24;
        static final int TRANSACTION_isRequestPinAppWidgetSupported = 26;
        static final int TRANSACTION_noteAppWidgetTapped = 27;
        static final int TRANSACTION_notifyAppWidgetViewDataChanged = 16;
        static final int TRANSACTION_partiallyUpdateAppWidgetIds = 13;
        static final int TRANSACTION_requestPinAppWidget = 25;
        static final int TRANSACTION_setBindAppWidgetPermission = 20;
        static final int TRANSACTION_startListening = 1;
        static final int TRANSACTION_stopListening = 2;
        static final int TRANSACTION_updateAppWidgetIds = 10;
        static final int TRANSACTION_updateAppWidgetOptions = 11;
        static final int TRANSACTION_updateAppWidgetProvider = 14;
        static final int TRANSACTION_updateAppWidgetProviderInfo = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAppWidgetService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAppWidgetService)) {
                return (IAppWidgetService) iin;
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
                    return "startListening";
                case 2:
                    return "stopListening";
                case 3:
                    return "allocateAppWidgetId";
                case 4:
                    return "deleteAppWidgetId";
                case 5:
                    return "deleteHost";
                case 6:
                    return "deleteAllHosts";
                case 7:
                    return "getAppWidgetViews";
                case 8:
                    return "getAppWidgetIdsForHost";
                case 9:
                    return "createAppWidgetConfigIntentSender";
                case 10:
                    return "updateAppWidgetIds";
                case 11:
                    return "updateAppWidgetOptions";
                case 12:
                    return "getAppWidgetOptions";
                case 13:
                    return "partiallyUpdateAppWidgetIds";
                case 14:
                    return "updateAppWidgetProvider";
                case 15:
                    return "updateAppWidgetProviderInfo";
                case 16:
                    return "notifyAppWidgetViewDataChanged";
                case 17:
                    return "getInstalledProvidersForProfile";
                case 18:
                    return "getAppWidgetInfo";
                case 19:
                    return "hasBindAppWidgetPermission";
                case 20:
                    return "setBindAppWidgetPermission";
                case 21:
                    return "bindAppWidgetId";
                case 22:
                    return "bindRemoteViewsService";
                case 23:
                    return "getAppWidgetIds";
                case 24:
                    return "isBoundWidgetPackage";
                case 25:
                    return "requestPinAppWidget";
                case 26:
                    return "isRequestPinAppWidgetSupported";
                case 27:
                    return "noteAppWidgetTapped";
                case 28:
                    return "getAppWidgetOfHost";
                case 29:
                    return "getBoundAppWidgets";
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
                            IAppWidgetHost _arg0 = IAppWidgetHost.Stub.asInterface(data.readStrongBinder());
                            String _arg1 = data.readString();
                            int _arg2 = data.readInt();
                            int[] _arg3 = data.createIntArray();
                            data.enforceNoDataAvail();
                            ParceledListSlice _result = startListening(_arg0, _arg1, _arg2, _arg3);
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            stopListening(_arg02, _arg12);
                            reply.writeNoException();
                            break;
                        case 3:
                            String _arg03 = data.readString();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            int _result2 = allocateAppWidgetId(_arg03, _arg13);
                            reply.writeNoException();
                            reply.writeInt(_result2);
                            break;
                        case 4:
                            String _arg04 = data.readString();
                            int _arg14 = data.readInt();
                            data.enforceNoDataAvail();
                            deleteAppWidgetId(_arg04, _arg14);
                            reply.writeNoException();
                            break;
                        case 5:
                            String _arg05 = data.readString();
                            int _arg15 = data.readInt();
                            data.enforceNoDataAvail();
                            deleteHost(_arg05, _arg15);
                            reply.writeNoException();
                            break;
                        case 6:
                            deleteAllHosts();
                            reply.writeNoException();
                            break;
                        case 7:
                            String _arg06 = data.readString();
                            int _arg16 = data.readInt();
                            data.enforceNoDataAvail();
                            RemoteViews _result3 = getAppWidgetViews(_arg06, _arg16);
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        case 8:
                            String _arg07 = data.readString();
                            int _arg17 = data.readInt();
                            data.enforceNoDataAvail();
                            int[] _result4 = getAppWidgetIdsForHost(_arg07, _arg17);
                            reply.writeNoException();
                            reply.writeIntArray(_result4);
                            break;
                        case 9:
                            String _arg08 = data.readString();
                            int _arg18 = data.readInt();
                            int _arg22 = data.readInt();
                            data.enforceNoDataAvail();
                            IntentSender _result5 = createAppWidgetConfigIntentSender(_arg08, _arg18, _arg22);
                            reply.writeNoException();
                            reply.writeTypedObject(_result5, 1);
                            break;
                        case 10:
                            String _arg09 = data.readString();
                            int[] _arg19 = data.createIntArray();
                            RemoteViews _arg23 = (RemoteViews) data.readTypedObject(RemoteViews.CREATOR);
                            data.enforceNoDataAvail();
                            updateAppWidgetIds(_arg09, _arg19, _arg23);
                            reply.writeNoException();
                            break;
                        case 11:
                            String _arg010 = data.readString();
                            int _arg110 = data.readInt();
                            Bundle _arg24 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            updateAppWidgetOptions(_arg010, _arg110, _arg24);
                            reply.writeNoException();
                            break;
                        case 12:
                            String _arg011 = data.readString();
                            int _arg111 = data.readInt();
                            data.enforceNoDataAvail();
                            Bundle _result6 = getAppWidgetOptions(_arg011, _arg111);
                            reply.writeNoException();
                            reply.writeTypedObject(_result6, 1);
                            break;
                        case 13:
                            String _arg012 = data.readString();
                            int[] _arg112 = data.createIntArray();
                            RemoteViews _arg25 = (RemoteViews) data.readTypedObject(RemoteViews.CREATOR);
                            data.enforceNoDataAvail();
                            partiallyUpdateAppWidgetIds(_arg012, _arg112, _arg25);
                            reply.writeNoException();
                            break;
                        case 14:
                            ComponentName _arg013 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            RemoteViews _arg113 = (RemoteViews) data.readTypedObject(RemoteViews.CREATOR);
                            data.enforceNoDataAvail();
                            updateAppWidgetProvider(_arg013, _arg113);
                            reply.writeNoException();
                            break;
                        case 15:
                            ComponentName _arg014 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            String _arg114 = data.readString();
                            data.enforceNoDataAvail();
                            updateAppWidgetProviderInfo(_arg014, _arg114);
                            reply.writeNoException();
                            break;
                        case 16:
                            String _arg015 = data.readString();
                            int[] _arg115 = data.createIntArray();
                            int _arg26 = data.readInt();
                            data.enforceNoDataAvail();
                            notifyAppWidgetViewDataChanged(_arg015, _arg115, _arg26);
                            reply.writeNoException();
                            break;
                        case 17:
                            int _arg016 = data.readInt();
                            int _arg116 = data.readInt();
                            String _arg27 = data.readString();
                            data.enforceNoDataAvail();
                            ParceledListSlice _result7 = getInstalledProvidersForProfile(_arg016, _arg116, _arg27);
                            reply.writeNoException();
                            reply.writeTypedObject(_result7, 1);
                            break;
                        case 18:
                            String _arg017 = data.readString();
                            int _arg117 = data.readInt();
                            data.enforceNoDataAvail();
                            AppWidgetProviderInfo _result8 = getAppWidgetInfo(_arg017, _arg117);
                            reply.writeNoException();
                            reply.writeTypedObject(_result8, 1);
                            break;
                        case 19:
                            String _arg018 = data.readString();
                            int _arg118 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result9 = hasBindAppWidgetPermission(_arg018, _arg118);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 20:
                            String _arg019 = data.readString();
                            int _arg119 = data.readInt();
                            boolean _arg28 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setBindAppWidgetPermission(_arg019, _arg119, _arg28);
                            reply.writeNoException();
                            break;
                        case 21:
                            String _arg020 = data.readString();
                            int _arg120 = data.readInt();
                            int _arg29 = data.readInt();
                            ComponentName _arg32 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            Bundle _arg4 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result10 = bindAppWidgetId(_arg020, _arg120, _arg29, _arg32, _arg4);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 22:
                            String _arg021 = data.readString();
                            int _arg121 = data.readInt();
                            Intent _arg210 = (Intent) data.readTypedObject(Intent.CREATOR);
                            IApplicationThread _arg33 = IApplicationThread.Stub.asInterface(data.readStrongBinder());
                            IBinder _arg42 = data.readStrongBinder();
                            IServiceConnection _arg5 = IServiceConnection.Stub.asInterface(data.readStrongBinder());
                            int _arg6 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result11 = bindRemoteViewsService(_arg021, _arg121, _arg210, _arg33, _arg42, _arg5, _arg6);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 23:
                            ComponentName _arg022 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            data.enforceNoDataAvail();
                            int[] _result12 = getAppWidgetIds(_arg022);
                            reply.writeNoException();
                            reply.writeIntArray(_result12);
                            break;
                        case 24:
                            String _arg023 = data.readString();
                            int _arg122 = data.readInt();
                            data.enforceNoDataAvail();
                            boolean _result13 = isBoundWidgetPackage(_arg023, _arg122);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case 25:
                            String _arg024 = data.readString();
                            ComponentName _arg123 = (ComponentName) data.readTypedObject(ComponentName.CREATOR);
                            Bundle _arg211 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            IntentSender _arg34 = (IntentSender) data.readTypedObject(IntentSender.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result14 = requestPinAppWidget(_arg024, _arg123, _arg211, _arg34);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case 26:
                            boolean _result15 = isRequestPinAppWidgetSupported();
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        case 27:
                            String _arg025 = data.readString();
                            int _arg124 = data.readInt();
                            data.enforceNoDataAvail();
                            noteAppWidgetTapped(_arg025, _arg124);
                            break;
                        case 28:
                            String _arg026 = data.readString();
                            int _arg125 = data.readInt();
                            data.enforceNoDataAvail();
                            List<ComponentName> _result16 = getAppWidgetOfHost(_arg026, _arg125);
                            reply.writeNoException();
                            reply.writeTypedList(_result16);
                            break;
                        case 29:
                            String _arg027 = data.readString();
                            data.enforceNoDataAvail();
                            List<String> _result17 = getBoundAppWidgets(_arg027);
                            reply.writeNoException();
                            reply.writeStringList(_result17);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes4.dex */
        public static class Proxy implements IAppWidgetService {
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

            @Override // com.android.internal.appwidget.IAppWidgetService
            public ParceledListSlice startListening(IAppWidgetHost host, String callingPackage, int hostId, int[] appWidgetIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongInterface(host);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    _data.writeIntArray(appWidgetIds);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void stopListening(String callingPackage, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public int allocateAppWidgetId(String callingPackage, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void deleteAppWidgetId(String callingPackage, int appWidgetId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void deleteHost(String packageName, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(hostId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void deleteAllHosts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    RemoteViews _result = (RemoteViews) _reply.readTypedObject(RemoteViews.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public int[] getAppWidgetIdsForHost(String callingPackage, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId, int intentFlags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    _data.writeInt(intentFlags);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    IntentSender _result = (IntentSender) _reply.readTypedObject(IntentSender.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeIntArray(appWidgetIds);
                    _data.writeTypedObject(views, 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    _data.writeTypedObject(extras, 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public Bundle getAppWidgetOptions(String callingPackage, int appWidgetId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    Bundle _result = (Bundle) _reply.readTypedObject(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeIntArray(appWidgetIds);
                    _data.writeTypedObject(views, 0);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void updateAppWidgetProvider(ComponentName provider, RemoteViews views) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(provider, 0);
                    _data.writeTypedObject(views, 0);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void updateAppWidgetProviderInfo(ComponentName provider, String metadataKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(provider, 0);
                    _data.writeString(metadataKey);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void notifyAppWidgetViewDataChanged(String packageName, int[] appWidgetIds, int viewId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeIntArray(appWidgetIds);
                    _data.writeInt(viewId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public ParceledListSlice getInstalledProvidersForProfile(int categoryFilter, int profileId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(categoryFilter);
                    _data.writeInt(profileId);
                    _data.writeString(packageName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    ParceledListSlice _result = (ParceledListSlice) _reply.readTypedObject(ParceledListSlice.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    AppWidgetProviderInfo _result = (AppWidgetProviderInfo) _reply.readTypedObject(AppWidgetProviderInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public boolean hasBindAppWidgetPermission(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void setBindAppWidgetPermission(String packageName, int userId, boolean permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeBoolean(permission);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public boolean bindAppWidgetId(String callingPackage, int appWidgetId, int providerProfileId, ComponentName providerComponent, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    _data.writeInt(providerProfileId);
                    _data.writeTypedObject(providerComponent, 0);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public boolean bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent, IApplicationThread caller, IBinder token, IServiceConnection connection, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    _data.writeTypedObject(intent, 0);
                    _data.writeStrongInterface(caller);
                    _data.writeStrongBinder(token);
                    _data.writeStrongInterface(connection);
                    _data.writeInt(flags);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public int[] getAppWidgetIds(ComponentName providerComponent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedObject(providerComponent, 0);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public boolean isBoundWidgetPackage(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public boolean requestPinAppWidget(String packageName, ComponentName providerComponent, Bundle extras, IntentSender resultIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeTypedObject(providerComponent, 0);
                    _data.writeTypedObject(extras, 0);
                    _data.writeTypedObject(resultIntent, 0);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public boolean isRequestPinAppWidgetSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public void noteAppWidgetTapped(String callingPackage, int appWidgetId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public List<ComponentName> getAppWidgetOfHost(String pkg, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    List<ComponentName> _result = _reply.createTypedArrayList(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.appwidget.IAppWidgetService
            public List<String> getBoundAppWidgets(String host) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(host);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
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
