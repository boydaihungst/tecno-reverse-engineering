package android.hardware.camera2.extension;

import android.hardware.camera2.extension.ICaptureProcessorImpl;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes.dex */
public interface IImageCaptureExtenderImpl extends IInterface {
    public static final String DESCRIPTOR = "android.hardware.camera2.extension.IImageCaptureExtenderImpl";

    CameraMetadataNative getAvailableCaptureRequestKeys() throws RemoteException;

    CameraMetadataNative getAvailableCaptureResultKeys() throws RemoteException;

    ICaptureProcessorImpl getCaptureProcessor() throws RemoteException;

    List<CaptureStageImpl> getCaptureStages() throws RemoteException;

    LatencyRange getEstimatedCaptureLatencyRange(Size size) throws RemoteException;

    int getMaxCaptureStage() throws RemoteException;

    List<SizeList> getSupportedResolutions() throws RemoteException;

    void init(String str, CameraMetadataNative cameraMetadataNative) throws RemoteException;

    boolean isExtensionAvailable(String str, CameraMetadataNative cameraMetadataNative) throws RemoteException;

    void onDeInit() throws RemoteException;

    CaptureStageImpl onDisableSession() throws RemoteException;

    CaptureStageImpl onEnableSession() throws RemoteException;

    void onInit(String str, CameraMetadataNative cameraMetadataNative) throws RemoteException;

    CaptureStageImpl onPresetSession() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IImageCaptureExtenderImpl {
        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public void onInit(String cameraId, CameraMetadataNative cameraCharacteristics) throws RemoteException {
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public void onDeInit() throws RemoteException {
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public CaptureStageImpl onPresetSession() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public CaptureStageImpl onEnableSession() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public CaptureStageImpl onDisableSession() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public boolean isExtensionAvailable(String cameraId, CameraMetadataNative chars) throws RemoteException {
            return false;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public void init(String cameraId, CameraMetadataNative chars) throws RemoteException {
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public ICaptureProcessorImpl getCaptureProcessor() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public List<CaptureStageImpl> getCaptureStages() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public int getMaxCaptureStage() throws RemoteException {
            return 0;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public List<SizeList> getSupportedResolutions() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public LatencyRange getEstimatedCaptureLatencyRange(Size outputSize) throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public CameraMetadataNative getAvailableCaptureRequestKeys() throws RemoteException {
            return null;
        }

        @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
        public CameraMetadataNative getAvailableCaptureResultKeys() throws RemoteException {
            return null;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IImageCaptureExtenderImpl {
        static final int TRANSACTION_getAvailableCaptureRequestKeys = 13;
        static final int TRANSACTION_getAvailableCaptureResultKeys = 14;
        static final int TRANSACTION_getCaptureProcessor = 8;
        static final int TRANSACTION_getCaptureStages = 9;
        static final int TRANSACTION_getEstimatedCaptureLatencyRange = 12;
        static final int TRANSACTION_getMaxCaptureStage = 10;
        static final int TRANSACTION_getSupportedResolutions = 11;
        static final int TRANSACTION_init = 7;
        static final int TRANSACTION_isExtensionAvailable = 6;
        static final int TRANSACTION_onDeInit = 2;
        static final int TRANSACTION_onDisableSession = 5;
        static final int TRANSACTION_onEnableSession = 4;
        static final int TRANSACTION_onInit = 1;
        static final int TRANSACTION_onPresetSession = 3;

        public Stub() {
            attachInterface(this, IImageCaptureExtenderImpl.DESCRIPTOR);
        }

        public static IImageCaptureExtenderImpl asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IImageCaptureExtenderImpl.DESCRIPTOR);
            if (iin != null && (iin instanceof IImageCaptureExtenderImpl)) {
                return (IImageCaptureExtenderImpl) iin;
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
                    return "onInit";
                case 2:
                    return "onDeInit";
                case 3:
                    return "onPresetSession";
                case 4:
                    return "onEnableSession";
                case 5:
                    return "onDisableSession";
                case 6:
                    return "isExtensionAvailable";
                case 7:
                    return "init";
                case 8:
                    return "getCaptureProcessor";
                case 9:
                    return "getCaptureStages";
                case 10:
                    return "getMaxCaptureStage";
                case 11:
                    return "getSupportedResolutions";
                case 12:
                    return "getEstimatedCaptureLatencyRange";
                case 13:
                    return "getAvailableCaptureRequestKeys";
                case 14:
                    return "getAvailableCaptureResultKeys";
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
                data.enforceInterface(IImageCaptureExtenderImpl.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IImageCaptureExtenderImpl.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            String _arg0 = data.readString();
                            CameraMetadataNative _arg1 = (CameraMetadataNative) data.readTypedObject(CameraMetadataNative.CREATOR);
                            data.enforceNoDataAvail();
                            onInit(_arg0, _arg1);
                            reply.writeNoException();
                            break;
                        case 2:
                            onDeInit();
                            reply.writeNoException();
                            break;
                        case 3:
                            CaptureStageImpl _result = onPresetSession();
                            reply.writeNoException();
                            reply.writeTypedObject(_result, 1);
                            break;
                        case 4:
                            CaptureStageImpl _result2 = onEnableSession();
                            reply.writeNoException();
                            reply.writeTypedObject(_result2, 1);
                            break;
                        case 5:
                            CaptureStageImpl _result3 = onDisableSession();
                            reply.writeNoException();
                            reply.writeTypedObject(_result3, 1);
                            break;
                        case 6:
                            String _arg02 = data.readString();
                            CameraMetadataNative _arg12 = (CameraMetadataNative) data.readTypedObject(CameraMetadataNative.CREATOR);
                            data.enforceNoDataAvail();
                            boolean _result4 = isExtensionAvailable(_arg02, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 7:
                            String _arg03 = data.readString();
                            CameraMetadataNative _arg13 = (CameraMetadataNative) data.readTypedObject(CameraMetadataNative.CREATOR);
                            data.enforceNoDataAvail();
                            init(_arg03, _arg13);
                            reply.writeNoException();
                            break;
                        case 8:
                            ICaptureProcessorImpl _result5 = getCaptureProcessor();
                            reply.writeNoException();
                            reply.writeStrongInterface(_result5);
                            break;
                        case 9:
                            List<CaptureStageImpl> _result6 = getCaptureStages();
                            reply.writeNoException();
                            reply.writeTypedList(_result6);
                            break;
                        case 10:
                            int _result7 = getMaxCaptureStage();
                            reply.writeNoException();
                            reply.writeInt(_result7);
                            break;
                        case 11:
                            List<SizeList> _result8 = getSupportedResolutions();
                            reply.writeNoException();
                            reply.writeTypedList(_result8);
                            break;
                        case 12:
                            Size _arg04 = (Size) data.readTypedObject(Size.CREATOR);
                            data.enforceNoDataAvail();
                            LatencyRange _result9 = getEstimatedCaptureLatencyRange(_arg04);
                            reply.writeNoException();
                            reply.writeTypedObject(_result9, 1);
                            break;
                        case 13:
                            CameraMetadataNative _result10 = getAvailableCaptureRequestKeys();
                            reply.writeNoException();
                            reply.writeTypedObject(_result10, 1);
                            break;
                        case 14:
                            CameraMetadataNative _result11 = getAvailableCaptureResultKeys();
                            reply.writeNoException();
                            reply.writeTypedObject(_result11, 1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes.dex */
        private static class Proxy implements IImageCaptureExtenderImpl {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IImageCaptureExtenderImpl.DESCRIPTOR;
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public void onInit(String cameraId, CameraMetadataNative cameraCharacteristics) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    _data.writeString(cameraId);
                    _data.writeTypedObject(cameraCharacteristics, 0);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public void onDeInit() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public CaptureStageImpl onPresetSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    CaptureStageImpl _result = (CaptureStageImpl) _reply.readTypedObject(CaptureStageImpl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public CaptureStageImpl onEnableSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    CaptureStageImpl _result = (CaptureStageImpl) _reply.readTypedObject(CaptureStageImpl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public CaptureStageImpl onDisableSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    CaptureStageImpl _result = (CaptureStageImpl) _reply.readTypedObject(CaptureStageImpl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public boolean isExtensionAvailable(String cameraId, CameraMetadataNative chars) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    _data.writeString(cameraId);
                    _data.writeTypedObject(chars, 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public void init(String cameraId, CameraMetadataNative chars) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    _data.writeString(cameraId);
                    _data.writeTypedObject(chars, 0);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public ICaptureProcessorImpl getCaptureProcessor() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    ICaptureProcessorImpl _result = ICaptureProcessorImpl.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public List<CaptureStageImpl> getCaptureStages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    List<CaptureStageImpl> _result = _reply.createTypedArrayList(CaptureStageImpl.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public int getMaxCaptureStage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public List<SizeList> getSupportedResolutions() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    List<SizeList> _result = _reply.createTypedArrayList(SizeList.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public LatencyRange getEstimatedCaptureLatencyRange(Size outputSize) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    _data.writeTypedObject(outputSize, 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    LatencyRange _result = (LatencyRange) _reply.readTypedObject(LatencyRange.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public CameraMetadataNative getAvailableCaptureRequestKeys() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    CameraMetadataNative _result = (CameraMetadataNative) _reply.readTypedObject(CameraMetadataNative.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // android.hardware.camera2.extension.IImageCaptureExtenderImpl
            public CameraMetadataNative getAvailableCaptureResultKeys() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IImageCaptureExtenderImpl.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    CameraMetadataNative _result = (CameraMetadataNative) _reply.readTypedObject(CameraMetadataNative.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
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
