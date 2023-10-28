package android.hardware.camera2.extension;

import android.hardware.camera2.extension.IProcessResultImpl;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Surface;
import java.util.List;
/* loaded from: classes.dex */
public interface ICaptureProcessorImpl extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.camera2.extension.ICaptureProcessorImpl";

    void onImageFormatUpdate(int i) throws RemoteException;

    void onOutputSurface(Surface surface, int i) throws RemoteException;

    void onResolutionUpdate(Size size) throws RemoteException;

    void process(List<CaptureBundle> list, IProcessResultImpl iProcessResultImpl) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements ICaptureProcessorImpl {
        @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
        public void onOutputSurface(Surface surface, int imageFormat) throws RemoteException {
        }

        @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
        public void onResolutionUpdate(Size size) throws RemoteException {
        }

        @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
        public void onImageFormatUpdate(int imageFormat) throws RemoteException {
        }

        @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
        public void process(List<CaptureBundle> capturelist, IProcessResultImpl resultCallback) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ICaptureProcessorImpl {
        static final int TRANSACTION_onImageFormatUpdate = 3;
        static final int TRANSACTION_onOutputSurface = 1;
        static final int TRANSACTION_onResolutionUpdate = 2;
        static final int TRANSACTION_process = 4;

        public Stub() {
            attachInterface(this, ICaptureProcessorImpl.DESCRIPTOR);
        }

        public static ICaptureProcessorImpl asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(ICaptureProcessorImpl.DESCRIPTOR);
            if (iin != null && (iin instanceof ICaptureProcessorImpl)) {
                return (ICaptureProcessorImpl) iin;
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
                    return "onOutputSurface";
                case 2:
                    return "onResolutionUpdate";
                case 3:
                    return "onImageFormatUpdate";
                case 4:
                    return "process";
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
                data.enforceInterface(ICaptureProcessorImpl.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(ICaptureProcessorImpl.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            Surface _arg0 = (Surface) data.readTypedObject(Surface.CREATOR);
                            int _arg1 = data.readInt();
                            data.enforceNoDataAvail();
                            onOutputSurface(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            Size _arg02 = (Size) data.readTypedObject(Size.CREATOR);
                            data.enforceNoDataAvail();
                            onResolutionUpdate(_arg02);
                            reply.writeNoException();
                            break;
                        case 3:
                            int _arg03 = data.readInt();
                            data.enforceNoDataAvail();
                            onImageFormatUpdate(_arg03);
                            reply.writeNoException();
                            break;
                        case 4:
                            List<CaptureBundle> _arg04 = data.createTypedArrayList(CaptureBundle.CREATOR);
                            IProcessResultImpl _arg12 = IProcessResultImpl.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            process(_arg04, _arg12);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements ICaptureProcessorImpl {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICaptureProcessorImpl.DESCRIPTOR;
            }

            @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
            public void onOutputSurface(Surface surface, int imageFormat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICaptureProcessorImpl.DESCRIPTOR);
                    _data.writeTypedObject(surface, 0);
                    _data.writeInt(imageFormat);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
            public void onResolutionUpdate(Size size) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICaptureProcessorImpl.DESCRIPTOR);
                    _data.writeTypedObject(size, 0);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
            public void onImageFormatUpdate(int imageFormat) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICaptureProcessorImpl.DESCRIPTOR);
                    _data.writeInt(imageFormat);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.ICaptureProcessorImpl
            public void process(List<CaptureBundle> capturelist, IProcessResultImpl resultCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(ICaptureProcessorImpl.DESCRIPTOR);
                    _data.writeTypedList(capturelist);
                    _data.writeStrongInterface(resultCallback);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 3;
        }
    }
}
