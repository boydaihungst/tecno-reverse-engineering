package com.transsion.trancare.handler.uxdetector;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public interface IUxDetectorService extends IInterface {
    public static final String DESCRIPTOR = "com.transsion.trancare.handler.uxdetector.IUxDetectorService";

    void appSwitchEvent(Bundle bundle) throws RemoteException;

    Map getAppProbabilities(List<String> list, int i) throws RemoteException;

    String getAppTimeRecommend() throws RemoteException;

    double getAppprobability(String str, int i) throws RemoteException;

    List<String> getAppsArrayNotRecommend(int i, int i2, int i3) throws RemoteException;

    List<String> getAppsArrayRecommend(int i, int i2) throws RemoteException;

    void setDebugMode(boolean z) throws RemoteException;

    /* loaded from: classes2.dex */
    public static class Default implements IUxDetectorService {
        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public void appSwitchEvent(Bundle switchEvent) throws RemoteException {
        }

        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public double getAppprobability(String appName, int flags) throws RemoteException {
            return 0.0d;
        }

        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public Map getAppProbabilities(List<String> appNames, int flags) throws RemoteException {
            return null;
        }

        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public List<String> getAppsArrayRecommend(int appNumbers, int flags) throws RemoteException {
            return null;
        }

        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public List<String> getAppsArrayNotRecommend(int hours, int appNumbers, int flags) throws RemoteException {
            return null;
        }

        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public void setDebugMode(boolean debug) throws RemoteException {
        }

        @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
        public String getAppTimeRecommend() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends Binder implements IUxDetectorService {
        static final int TRANSACTION_appSwitchEvent = 1;
        static final int TRANSACTION_getAppProbabilities = 3;
        static final int TRANSACTION_getAppTimeRecommend = 7;
        static final int TRANSACTION_getAppprobability = 2;
        static final int TRANSACTION_getAppsArrayNotRecommend = 5;
        static final int TRANSACTION_getAppsArrayRecommend = 4;
        static final int TRANSACTION_setDebugMode = 6;

        public Stub() {
            attachInterface(this, IUxDetectorService.DESCRIPTOR);
        }

        public static IUxDetectorService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IUxDetectorService.DESCRIPTOR);
            if (iin != null && (iin instanceof IUxDetectorService)) {
                return (IUxDetectorService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IUxDetectorService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IUxDetectorService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Bundle _arg0 = (Bundle) data.readTypedObject(Bundle.CREATOR);
                            data.enforceNoDataAvail();
                            appSwitchEvent(_arg0);
                            reply.writeNoException();
                            break;
                        case 2:
                            String _arg02 = data.readString();
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            double _result = getAppprobability(_arg02, _arg1);
                            reply.writeNoException();
                            reply.writeDouble(_result);
                            break;
                        case 3:
                            List<String> _arg03 = data.createStringArrayList();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            Map _result2 = getAppProbabilities(_arg03, _arg12);
                            reply.writeNoException();
                            reply.writeMap(_result2);
                            break;
                        case 4:
                            int _arg04 = data.readInt();
                            int _arg13 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result3 = getAppsArrayRecommend(_arg04, _arg13);
                            reply.writeNoException();
                            reply.writeStringList(_result3);
                            break;
                        case 5:
                            int _arg05 = data.readInt();
                            int _arg14 = data.readInt();
                            int _arg2 = data.readInt();
                            data.enforceNoDataAvail();
                            List<String> _result4 = getAppsArrayNotRecommend(_arg05, _arg14, _arg2);
                            reply.writeNoException();
                            reply.writeStringList(_result4);
                            break;
                        case 6:
                            boolean _arg06 = data.readBoolean();
                            data.enforceNoDataAvail();
                            setDebugMode(_arg06);
                            reply.writeNoException();
                            break;
                        case 7:
                            String _result5 = getAppTimeRecommend();
                            reply.writeNoException();
                            reply.writeString(_result5);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes2.dex */
        private static class Proxy implements IUxDetectorService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IUxDetectorService.DESCRIPTOR;
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public void appSwitchEvent(Bundle switchEvent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    _data.writeTypedObject(switchEvent, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public double getAppprobability(String appName, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    _data.writeString(appName);
                    _data.writeInt(flags);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    double _result = _reply.readDouble();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public Map getAppProbabilities(List<String> appNames, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    _data.writeStringList(appNames);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    ClassLoader cl = getClass().getClassLoader();
                    Map _result = _reply.readHashMap(cl);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public List<String> getAppsArrayRecommend(int appNumbers, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    _data.writeInt(appNumbers);
                    _data.writeInt(flags);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public List<String> getAppsArrayNotRecommend(int hours, int appNumbers, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    _data.writeInt(hours);
                    _data.writeInt(appNumbers);
                    _data.writeInt(flags);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public void setDebugMode(boolean debug) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    _data.writeBoolean(debug);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.transsion.trancare.handler.uxdetector.IUxDetectorService
            public String getAppTimeRecommend() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IUxDetectorService.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
