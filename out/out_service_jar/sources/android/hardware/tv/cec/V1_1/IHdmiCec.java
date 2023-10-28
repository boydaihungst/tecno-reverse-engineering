package android.hardware.tv.cec.V1_1;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hardware.tv.cec.V1_0.HdmiPortInfo;
import android.hardware.tv.cec.V1_0.IHdmiCec;
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
public interface IHdmiCec extends android.hardware.tv.cec.V1_0.IHdmiCec {
    public static final String kInterfaceName = "android.hardware.tv.cec@1.1::IHdmiCec";

    int addLogicalAddress_1_1(int i) throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    int sendMessage_1_1(CecMessage cecMessage) throws RemoteException;

    void setCallback_1_1(IHdmiCecCallback iHdmiCecCallback) throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IHdmiCec asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IHdmiCec)) {
            return (IHdmiCec) iface;
        }
        IHdmiCec proxy = new Proxy(binder);
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

    static IHdmiCec castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IHdmiCec getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IHdmiCec getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IHdmiCec getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IHdmiCec getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IHdmiCec {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.tv.cec@1.1::IHdmiCec]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public int addLogicalAddress(int addr) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            _hidl_request.writeInt32(addr);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public void clearLogicalAddress() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public void getPhysicalAddress(IHdmiCec.getPhysicalAddressCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                short _hidl_out_addr = _hidl_reply.readInt16();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_addr);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public int sendMessage(android.hardware.tv.cec.V1_0.CecMessage message) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            message.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public void setCallback(android.hardware.tv.cec.V1_0.IHdmiCecCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public int getCecVersion() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_version = _hidl_reply.readInt32();
                return _hidl_out_version;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public int getVendorId() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_vendorId = _hidl_reply.readInt32();
                return _hidl_out_vendorId;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public ArrayList<HdmiPortInfo> getPortInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<HdmiPortInfo> _hidl_out_infos = HdmiPortInfo.readVectorFromParcel(_hidl_reply);
                return _hidl_out_infos;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public void setOption(int key, boolean value) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            _hidl_request.writeInt32(key);
            _hidl_request.writeBool(value);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public void setLanguage(String language) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            _hidl_request.writeString(language);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public void enableAudioReturnChannel(int portId, boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            _hidl_request.writeInt32(portId);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_0.IHdmiCec
        public boolean isConnected(int portId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
            _hidl_request.writeInt32(portId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_status = _hidl_reply.readBool();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec
        public int addLogicalAddress_1_1(int addr) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHdmiCec.kInterfaceName);
            _hidl_request.writeInt32(addr);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec
        public int sendMessage_1_1(CecMessage message) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHdmiCec.kInterfaceName);
            message.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec
        public void setCallback_1_1(IHdmiCecCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHdmiCec.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IHdmiCec {
        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IHdmiCec.kInterfaceName, android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IHdmiCec.kInterfaceName;
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{99, -33, -37, 67, 58, -57, 63, -78, -65, 74, 68, -46, -83, -25, -73, -30, -119, -31, 85, UsbASFormat.EXT_FORMAT_TYPE_III, 82, 6, -47, -109, -106, 64, -42, -56, -115, 32, -119, -108}, new byte[]{-109, -120, 80, 98, 28, 60, 94, -12, 38, -92, -72, -114, 117, 43, -87, -101, 53, 89, 3, 126, 120, UsbDescriptor.DESCRIPTORTYPE_SUPERSPEED_HUB, 61, -109, -122, 4, -13, -82, -11, -52, 15, 27}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.tv.cec.V1_1.IHdmiCec, android.hardware.tv.cec.V1_0.IHdmiCec, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IHdmiCec.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    int addr = _hidl_request.readInt32();
                    int _hidl_out_result = addLogicalAddress(addr);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    clearLogicalAddress();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    getPhysicalAddress(new IHdmiCec.getPhysicalAddressCallback() { // from class: android.hardware.tv.cec.V1_1.IHdmiCec.Stub.1
                        @Override // android.hardware.tv.cec.V1_0.IHdmiCec.getPhysicalAddressCallback
                        public void onValues(int result, short addr2) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt16(addr2);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    android.hardware.tv.cec.V1_0.CecMessage message = new android.hardware.tv.cec.V1_0.CecMessage();
                    message.readFromParcel(_hidl_request);
                    int _hidl_out_result2 = sendMessage(message);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result2);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    android.hardware.tv.cec.V1_0.IHdmiCecCallback callback = android.hardware.tv.cec.V1_0.IHdmiCecCallback.asInterface(_hidl_request.readStrongBinder());
                    setCallback(callback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    int _hidl_out_version = getCecVersion();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_version);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    int _hidl_out_vendorId = getVendorId();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_vendorId);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    ArrayList<HdmiPortInfo> _hidl_out_infos = getPortInfo();
                    _hidl_reply.writeStatus(0);
                    HdmiPortInfo.writeVectorToParcel(_hidl_reply, _hidl_out_infos);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    int key = _hidl_request.readInt32();
                    boolean value = _hidl_request.readBool();
                    setOption(key, value);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    String language = _hidl_request.readString();
                    setLanguage(language);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    int portId = _hidl_request.readInt32();
                    boolean enable = _hidl_request.readBool();
                    enableAudioReturnChannel(portId, enable);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(android.hardware.tv.cec.V1_0.IHdmiCec.kInterfaceName);
                    int portId2 = _hidl_request.readInt32();
                    boolean _hidl_out_status = isConnected(portId2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_status);
                    _hidl_reply.send();
                    return;
                case 13:
                    _hidl_request.enforceInterface(IHdmiCec.kInterfaceName);
                    int addr2 = _hidl_request.readInt32();
                    int _hidl_out_result3 = addLogicalAddress_1_1(addr2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result3);
                    _hidl_reply.send();
                    return;
                case 14:
                    _hidl_request.enforceInterface(IHdmiCec.kInterfaceName);
                    CecMessage message2 = new CecMessage();
                    message2.readFromParcel(_hidl_request);
                    int _hidl_out_result4 = sendMessage_1_1(message2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result4);
                    _hidl_reply.send();
                    return;
                case 15:
                    _hidl_request.enforceInterface(IHdmiCec.kInterfaceName);
                    IHdmiCecCallback callback2 = IHdmiCecCallback.asInterface(_hidl_request.readStrongBinder());
                    setCallback_1_1(callback2);
                    _hidl_reply.writeStatus(0);
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
