package com.mediatek.advcam;

import android.hardware.camera2.CaptureRequest;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IAdvCamService extends IInterface {
    public static final String DESCRIPTOR = "com.mediatek.advcam.IAdvCamService";

    int setConfigureParam(int i, CaptureRequest captureRequest) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IAdvCamService {
        @Override // com.mediatek.advcam.IAdvCamService
        public int setConfigureParam(int openId, CaptureRequest request) throws RemoteException {
            return 0;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IAdvCamService {
        static final int TRANSACTION_setConfigureParam = 1;

        public Stub() {
            attachInterface(this, IAdvCamService.DESCRIPTOR);
        }

        public static IAdvCamService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IAdvCamService.DESCRIPTOR);
            if (iin != null && (iin instanceof IAdvCamService)) {
                return (IAdvCamService) iin;
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
                data.enforceInterface(IAdvCamService.DESCRIPTOR);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(IAdvCamService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int _arg0 = data.readInt();
                            CaptureRequest _arg1 = (CaptureRequest) data.readTypedObject(CaptureRequest.CREATOR);
                            data.enforceNoDataAvail();
                            int _result = setConfigureParam(_arg0, _arg1);
                            reply.writeNoException();
                            reply.writeInt(_result);
                            return true;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IAdvCamService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IAdvCamService.DESCRIPTOR;
            }

            @Override // com.mediatek.advcam.IAdvCamService
            public int setConfigureParam(int openId, CaptureRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IAdvCamService.DESCRIPTOR);
                    _data.writeInt(openId);
                    _data.writeTypedObject(request, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
