package android.service.voice;

import android.content.ContentCaptureOptions;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.service.voice.IDspHotwordDetectionCallback;
import android.view.contentcapture.IContentCaptureManager;
/* loaded from: classes3.dex */
public interface IHotwordDetectionService extends IInterface {
    public static final String DESCRIPTOR = "android.service.voice.IHotwordDetectionService";

    void detectFromDspSource(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent, AudioFormat audioFormat, long j, IDspHotwordDetectionCallback iDspHotwordDetectionCallback) throws RemoteException;

    void detectFromMicrophoneSource(ParcelFileDescriptor parcelFileDescriptor, int i, AudioFormat audioFormat, PersistableBundle persistableBundle, IDspHotwordDetectionCallback iDspHotwordDetectionCallback) throws RemoteException;

    void ping(IRemoteCallback iRemoteCallback) throws RemoteException;

    void stopDetection() throws RemoteException;

    void updateAudioFlinger(IBinder iBinder) throws RemoteException;

    void updateContentCaptureManager(IContentCaptureManager iContentCaptureManager, ContentCaptureOptions contentCaptureOptions) throws RemoteException;

    void updateState(PersistableBundle persistableBundle, SharedMemory sharedMemory, IRemoteCallback iRemoteCallback) throws RemoteException;

    /* loaded from: classes3.dex */
    public static class Default implements IHotwordDetectionService {
        @Override // android.service.voice.IHotwordDetectionService
        public void detectFromDspSource(SoundTrigger.KeyphraseRecognitionEvent event, AudioFormat audioFormat, long timeoutMillis, IDspHotwordDetectionCallback callback) throws RemoteException {
        }

        @Override // android.service.voice.IHotwordDetectionService
        public void detectFromMicrophoneSource(ParcelFileDescriptor audioStream, int audioSource, AudioFormat audioFormat, PersistableBundle options, IDspHotwordDetectionCallback callback) throws RemoteException {
        }

        @Override // android.service.voice.IHotwordDetectionService
        public void updateState(PersistableBundle options, SharedMemory sharedMemory, IRemoteCallback callback) throws RemoteException {
        }

        @Override // android.service.voice.IHotwordDetectionService
        public void updateAudioFlinger(IBinder audioFlinger) throws RemoteException {
        }

        @Override // android.service.voice.IHotwordDetectionService
        public void updateContentCaptureManager(IContentCaptureManager contentCaptureManager, ContentCaptureOptions options) throws RemoteException {
        }

        @Override // android.service.voice.IHotwordDetectionService
        public void ping(IRemoteCallback callback) throws RemoteException {
        }

        @Override // android.service.voice.IHotwordDetectionService
        public void stopDetection() throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Stub extends Binder implements IHotwordDetectionService {
        static final int TRANSACTION_detectFromDspSource = 1;
        static final int TRANSACTION_detectFromMicrophoneSource = 2;
        static final int TRANSACTION_ping = 6;
        static final int TRANSACTION_stopDetection = 7;
        static final int TRANSACTION_updateAudioFlinger = 4;
        static final int TRANSACTION_updateContentCaptureManager = 5;
        static final int TRANSACTION_updateState = 3;

        public Stub() {
            attachInterface(this, IHotwordDetectionService.DESCRIPTOR);
        }

        public static IHotwordDetectionService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(IHotwordDetectionService.DESCRIPTOR);
            if (iin != null && (iin instanceof IHotwordDetectionService)) {
                return (IHotwordDetectionService) iin;
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
                    return "detectFromDspSource";
                case 2:
                    return "detectFromMicrophoneSource";
                case 3:
                    return "updateState";
                case 4:
                    return "updateAudioFlinger";
                case 5:
                    return "updateContentCaptureManager";
                case 6:
                    return "ping";
                case 7:
                    return "stopDetection";
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
                data.enforceInterface(IHotwordDetectionService.DESCRIPTOR);
            }
            switch (code) {
                case IBinder.INTERFACE_TRANSACTION /* 1598968902 */:
                    reply.writeString(IHotwordDetectionService.DESCRIPTOR);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            SoundTrigger.KeyphraseRecognitionEvent _arg0 = (SoundTrigger.KeyphraseRecognitionEvent) data.readTypedObject(SoundTrigger.KeyphraseRecognitionEvent.CREATOR);
                            AudioFormat _arg1 = (AudioFormat) data.readTypedObject(AudioFormat.CREATOR);
                            long _arg2 = data.readLong();
                            IDspHotwordDetectionCallback _arg3 = IDspHotwordDetectionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            detectFromDspSource(_arg0, _arg1, _arg2, _arg3);
                            break;
                        case 2:
                            ParcelFileDescriptor _arg02 = (ParcelFileDescriptor) data.readTypedObject(ParcelFileDescriptor.CREATOR);
                            int _arg12 = data.readInt();
                            AudioFormat _arg22 = (AudioFormat) data.readTypedObject(AudioFormat.CREATOR);
                            PersistableBundle _arg32 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            IDspHotwordDetectionCallback _arg4 = IDspHotwordDetectionCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            detectFromMicrophoneSource(_arg02, _arg12, _arg22, _arg32, _arg4);
                            break;
                        case 3:
                            PersistableBundle _arg03 = (PersistableBundle) data.readTypedObject(PersistableBundle.CREATOR);
                            SharedMemory _arg13 = (SharedMemory) data.readTypedObject(SharedMemory.CREATOR);
                            IRemoteCallback _arg23 = IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            updateState(_arg03, _arg13, _arg23);
                            break;
                        case 4:
                            IBinder _arg04 = data.readStrongBinder();
                            data.enforceNoDataAvail();
                            updateAudioFlinger(_arg04);
                            break;
                        case 5:
                            IContentCaptureManager _arg05 = IContentCaptureManager.Stub.asInterface(data.readStrongBinder());
                            ContentCaptureOptions _arg14 = (ContentCaptureOptions) data.readTypedObject(ContentCaptureOptions.CREATOR);
                            data.enforceNoDataAvail();
                            updateContentCaptureManager(_arg05, _arg14);
                            break;
                        case 6:
                            IRemoteCallback _arg06 = IRemoteCallback.Stub.asInterface(data.readStrongBinder());
                            data.enforceNoDataAvail();
                            ping(_arg06);
                            break;
                        case 7:
                            stopDetection();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* loaded from: classes3.dex */
        private static class Proxy implements IHotwordDetectionService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IHotwordDetectionService.DESCRIPTOR;
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void detectFromDspSource(SoundTrigger.KeyphraseRecognitionEvent event, AudioFormat audioFormat, long timeoutMillis, IDspHotwordDetectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    _data.writeTypedObject(event, 0);
                    _data.writeTypedObject(audioFormat, 0);
                    _data.writeLong(timeoutMillis);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void detectFromMicrophoneSource(ParcelFileDescriptor audioStream, int audioSource, AudioFormat audioFormat, PersistableBundle options, IDspHotwordDetectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    _data.writeTypedObject(audioStream, 0);
                    _data.writeInt(audioSource);
                    _data.writeTypedObject(audioFormat, 0);
                    _data.writeTypedObject(options, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void updateState(PersistableBundle options, SharedMemory sharedMemory, IRemoteCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    _data.writeTypedObject(options, 0);
                    _data.writeTypedObject(sharedMemory, 0);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void updateAudioFlinger(IBinder audioFlinger) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    _data.writeStrongBinder(audioFlinger);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void updateContentCaptureManager(IContentCaptureManager contentCaptureManager, ContentCaptureOptions options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    _data.writeStrongInterface(contentCaptureManager);
                    _data.writeTypedObject(options, 0);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void ping(IRemoteCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    _data.writeStrongInterface(callback);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // android.service.voice.IHotwordDetectionService
            public void stopDetection() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(IHotwordDetectionService.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        @Override // android.os.Binder
        public int getMaxTransactionId() {
            return 6;
        }
    }
}
