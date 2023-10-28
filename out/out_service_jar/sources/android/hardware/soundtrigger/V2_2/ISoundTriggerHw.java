package android.hardware.soundtrigger.V2_2;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hardware.soundtrigger.V2_0.ISoundTriggerHw;
import android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback;
import android.hardware.soundtrigger.V2_1.ISoundTriggerHw;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.usb.descriptors.UsbASFormat;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
/* loaded from: classes.dex */
public interface ISoundTriggerHw extends android.hardware.soundtrigger.V2_1.ISoundTriggerHw {
    public static final String kInterfaceName = "android.hardware.soundtrigger@2.2::ISoundTriggerHw";

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    int getModelState(int i) throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISoundTriggerHw asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISoundTriggerHw)) {
            return (ISoundTriggerHw) iface;
        }
        ISoundTriggerHw proxy = new Proxy(binder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                String descriptor = it.next();
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static ISoundTriggerHw castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ISoundTriggerHw getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISoundTriggerHw getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static ISoundTriggerHw getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static ISoundTriggerHw getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements ISoundTriggerHw {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.soundtrigger@2.2::ISoundTriggerHw]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public void getProperties(ISoundTriggerHw.getPropertiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                ISoundTriggerHw.Properties _hidl_out_properties = new ISoundTriggerHw.Properties();
                _hidl_out_properties.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_properties);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public void loadSoundModel(ISoundTriggerHw.SoundModel soundModel, ISoundTriggerHwCallback callback, int cookie, ISoundTriggerHw.loadSoundModelCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            soundModel.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                int _hidl_out_modelHandle = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_modelHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public void loadPhraseSoundModel(ISoundTriggerHw.PhraseSoundModel soundModel, ISoundTriggerHwCallback callback, int cookie, ISoundTriggerHw.loadPhraseSoundModelCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            soundModel.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                int _hidl_out_modelHandle = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_modelHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int unloadSoundModel(int modelHandle) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int startRecognition(int modelHandle, ISoundTriggerHw.RecognitionConfig config, ISoundTriggerHwCallback callback, int cookie) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            config.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int stopRecognition(int modelHandle) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int stopAllRecognitions() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw
        public void loadSoundModel_2_1(ISoundTriggerHw.SoundModel soundModel, android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback callback, int cookie, ISoundTriggerHw.loadSoundModel_2_1Callback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName);
            soundModel.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                int _hidl_out_modelHandle = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_modelHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw
        public void loadPhraseSoundModel_2_1(ISoundTriggerHw.PhraseSoundModel soundModel, android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback callback, int cookie, ISoundTriggerHw.loadPhraseSoundModel_2_1Callback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName);
            soundModel.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                int _hidl_out_modelHandle = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_modelHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw
        public int startRecognition_2_1(int modelHandle, ISoundTriggerHw.RecognitionConfig config, android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback callback, int cookie) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            config.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw
        public int getModelState(int modelHandle) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256131655, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList<>();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16L);
                int _hidl_vec_size = _hidl_blob.getInt32(8L);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    byte[] _hidl_vec_element = new byte[32];
                    long _hidl_array_offset_1 = _hidl_index_0 * 32;
                    childBlob.copyToInt8Array(_hidl_array_offset_1, _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements ISoundTriggerHw {
        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISoundTriggerHw.kInterfaceName, android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName, android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return ISoundTriggerHw.kInterfaceName;
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-50, 75, -104, UsbDescriptor.DESCRIPTORTYPE_HID, AcquiredInfo.DARK_GLASSES_DETECTED, 89, 68, -109, 97, AcquiredInfo.FACE_OBSCURED, 109, 75, 30, 85, 84, -36, -124, 28, -21, 77, 69, 119, AcquiredInfo.START, 77, 123, 47, -74, -47, -21, 80, 79, 118}, new byte[]{-76, -11, 7, -76, -36, -101, 92, -43, -16, -28, 68, 89, 38, -84, -73, -39, 69, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_ENDPOINT, -82, 96, -36, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, 123, 57, 81, AcquiredInfo.FACE_OBSCURED, UsbDescriptor.DESCRIPTORTYPE_REPORT, UsbASFormat.EXT_FORMAT_TYPE_III, 99, UsbDescriptor.DESCRIPTORTYPE_REPORT, 7, -74}, new byte[]{91, -17, -64, AcquiredInfo.DARK_GLASSES_DETECTED, -53, -23, 73, 83, 102, 30, 44, -37, -107, -29, -49, 100, -11, -27, 101, -62, -108, 3, -31, -62, -38, -20, -62, -66, 68, -32, -91, 92}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.soundtrigger.V2_2.ISoundTriggerHw, android.hardware.soundtrigger.V2_1.ISoundTriggerHw, android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ISoundTriggerHw.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, final HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    getProperties(new ISoundTriggerHw.getPropertiesCallback() { // from class: android.hardware.soundtrigger.V2_2.ISoundTriggerHw.Stub.1
                        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.getPropertiesCallback
                        public void onValues(int retval, ISoundTriggerHw.Properties properties) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            properties.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    ISoundTriggerHw.SoundModel soundModel = new ISoundTriggerHw.SoundModel();
                    soundModel.readFromParcel(_hidl_request);
                    ISoundTriggerHwCallback callback = ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie = _hidl_request.readInt32();
                    loadSoundModel(soundModel, callback, cookie, new ISoundTriggerHw.loadSoundModelCallback() { // from class: android.hardware.soundtrigger.V2_2.ISoundTriggerHw.Stub.2
                        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.loadSoundModelCallback
                        public void onValues(int retval, int modelHandle) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt32(modelHandle);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    ISoundTriggerHw.PhraseSoundModel soundModel2 = new ISoundTriggerHw.PhraseSoundModel();
                    soundModel2.readFromParcel(_hidl_request);
                    ISoundTriggerHwCallback callback2 = ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie2 = _hidl_request.readInt32();
                    loadPhraseSoundModel(soundModel2, callback2, cookie2, new ISoundTriggerHw.loadPhraseSoundModelCallback() { // from class: android.hardware.soundtrigger.V2_2.ISoundTriggerHw.Stub.3
                        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.loadPhraseSoundModelCallback
                        public void onValues(int retval, int modelHandle) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt32(modelHandle);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    int modelHandle = _hidl_request.readInt32();
                    int _hidl_out_retval = unloadSoundModel(modelHandle);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    int modelHandle2 = _hidl_request.readInt32();
                    ISoundTriggerHw.RecognitionConfig config = new ISoundTriggerHw.RecognitionConfig();
                    config.readFromParcel(_hidl_request);
                    ISoundTriggerHwCallback callback3 = ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie3 = _hidl_request.readInt32();
                    int _hidl_out_retval2 = startRecognition(modelHandle2, config, callback3, cookie3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval2);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    int modelHandle3 = _hidl_request.readInt32();
                    int _hidl_out_retval3 = stopRecognition(modelHandle3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval3);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_0.ISoundTriggerHw.kInterfaceName);
                    int _hidl_out_retval4 = stopAllRecognitions();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval4);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName);
                    ISoundTriggerHw.SoundModel soundModel3 = new ISoundTriggerHw.SoundModel();
                    soundModel3.readFromParcel(_hidl_request);
                    android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback callback4 = android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie4 = _hidl_request.readInt32();
                    loadSoundModel_2_1(soundModel3, callback4, cookie4, new ISoundTriggerHw.loadSoundModel_2_1Callback() { // from class: android.hardware.soundtrigger.V2_2.ISoundTriggerHw.Stub.4
                        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw.loadSoundModel_2_1Callback
                        public void onValues(int retval, int modelHandle4) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt32(modelHandle4);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 9:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName);
                    ISoundTriggerHw.PhraseSoundModel soundModel4 = new ISoundTriggerHw.PhraseSoundModel();
                    soundModel4.readFromParcel(_hidl_request);
                    android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback callback5 = android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie5 = _hidl_request.readInt32();
                    loadPhraseSoundModel_2_1(soundModel4, callback5, cookie5, new ISoundTriggerHw.loadPhraseSoundModel_2_1Callback() { // from class: android.hardware.soundtrigger.V2_2.ISoundTriggerHw.Stub.5
                        @Override // android.hardware.soundtrigger.V2_1.ISoundTriggerHw.loadPhraseSoundModel_2_1Callback
                        public void onValues(int retval, int modelHandle4) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt32(modelHandle4);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 10:
                    _hidl_request.enforceInterface(android.hardware.soundtrigger.V2_1.ISoundTriggerHw.kInterfaceName);
                    int modelHandle4 = _hidl_request.readInt32();
                    ISoundTriggerHw.RecognitionConfig config2 = new ISoundTriggerHw.RecognitionConfig();
                    config2.readFromParcel(_hidl_request);
                    android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback callback6 = android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie6 = _hidl_request.readInt32();
                    int _hidl_out_retval5 = startRecognition_2_1(modelHandle4, config2, callback6, cookie6);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval5);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    int modelHandle5 = _hidl_request.readInt32();
                    int _hidl_out_retval6 = getModelState(modelHandle5);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval6);
                    _hidl_reply.send();
                    return;
                case 256067662:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ArrayList<String> _hidl_out_descriptors = interfaceChain();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStringVector(_hidl_out_descriptors);
                    _hidl_reply.send();
                    return;
                case 256131655:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    NativeHandle fd = _hidl_request.readNativeHandle();
                    ArrayList<String> options = _hidl_request.readStringVector();
                    debug(fd, options);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 256136003:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    String _hidl_out_descriptor = interfaceDescriptor();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_descriptor);
                    _hidl_reply.send();
                    return;
                case 256398152:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                    _hidl_reply.writeStatus(0);
                    HwBlob _hidl_blob = new HwBlob(16);
                    int _hidl_vec_size = _hidl_out_hashchain.size();
                    _hidl_blob.putInt32(8L, _hidl_vec_size);
                    _hidl_blob.putBool(12L, false);
                    HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                    for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                        long _hidl_array_offset_1 = _hidl_index_0 * 32;
                        byte[] _hidl_array_item_1 = _hidl_out_hashchain.get(_hidl_index_0);
                        if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                            throw new IllegalArgumentException("Array element is not of the expected length");
                        }
                        childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                    }
                    _hidl_blob.putBlob(0L, childBlob);
                    _hidl_reply.writeBuffer(_hidl_blob);
                    _hidl_reply.send();
                    return;
                case 256462420:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    setHALInstrumentation();
                    return;
                case 256660548:
                default:
                    return;
                case 256921159:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ping();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 257049926:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    DebugInfo _hidl_out_info = getDebugInfo();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_info.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 257120595:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    notifySyspropsChanged();
                    return;
            }
        }
    }
}
