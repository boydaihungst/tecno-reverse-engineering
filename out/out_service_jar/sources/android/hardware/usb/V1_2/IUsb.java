package android.hardware.usb.V1_2;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hardware.usb.V1_0.PortRole;
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
public interface IUsb extends android.hardware.usb.V1_1.IUsb {
    public static final String kInterfaceName = "android.hardware.usb@1.2::IUsb";

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    void enableContaminantPresenceDetection(String str, boolean z) throws RemoteException;

    void enableContaminantPresenceProtection(String str, boolean z) throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IUsb asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IUsb)) {
            return (IUsb) iface;
        }
        IUsb proxy = new Proxy(binder);
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

    static IUsb castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IUsb getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IUsb getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IUsb getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IUsb getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IUsb {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.usb@1.2::IUsb]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.usb.V1_0.IUsb
        public void switchRole(String portName, PortRole role) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.usb.V1_0.IUsb.kInterfaceName);
            _hidl_request.writeString(portName);
            role.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.usb.V1_0.IUsb
        public void setCallback(android.hardware.usb.V1_0.IUsbCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.usb.V1_0.IUsb.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.usb.V1_0.IUsb
        public void queryPortStatus() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.usb.V1_0.IUsb.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.usb.V1_2.IUsb
        public void enableContaminantPresenceDetection(String portName, boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IUsb.kInterfaceName);
            _hidl_request.writeString(portName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.usb.V1_2.IUsb
        public void enableContaminantPresenceProtection(String portName, boolean enable) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IUsb.kInterfaceName);
            _hidl_request.writeString(portName);
            _hidl_request.writeBool(enable);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IUsb {
        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IUsb.kInterfaceName, android.hardware.usb.V1_1.IUsb.kInterfaceName, android.hardware.usb.V1_0.IUsb.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IUsb.kInterfaceName;
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{97, -68, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, 46, 124, -105, 76, 89, -78, 88, -104, -59, -123, -58, -23, 104, 94, -118, UsbASFormat.EXT_FORMAT_TYPE_I, 2, 27, 27, -19, 62, -19, -11, UsbDescriptor.DESCRIPTORTYPE_REPORT, 65, -104, -14, 120, 90}, new byte[]{-82, -68, -39, -1, 45, -96, 92, -99, 76, 67, -103, AcquiredInfo.SENSOR_DIRTY, -12, 13, -3, UsbDescriptor.DESCRIPTORTYPE_HID, -101, -89, 98, -103, AcquiredInfo.DARK_GLASSES_DETECTED, 0, 124, -71, UsbASFormat.EXT_FORMAT_TYPE_I, -21, -15, 80, 6, 75, 79, UsbASFormat.EXT_FORMAT_TYPE_II}, new byte[]{78, -11, 116, -103, 39, 63, 56, -67, -67, -48, -63, 94, 86, -18, 122, 75, -59, -15, -118, 86, 68, 9, UsbDescriptor.DESCRIPTORTYPE_HID, 112, -91, 49, -33, 53, 65, -39, -32, AcquiredInfo.START}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.usb.V1_2.IUsb, android.hardware.usb.V1_1.IUsb, android.hardware.usb.V1_0.IUsb, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IUsb.kInterfaceName.equals(descriptor)) {
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

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(android.hardware.usb.V1_0.IUsb.kInterfaceName);
                    String portName = _hidl_request.readString();
                    PortRole role = new PortRole();
                    role.readFromParcel(_hidl_request);
                    switchRole(portName, role);
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hardware.usb.V1_0.IUsb.kInterfaceName);
                    android.hardware.usb.V1_0.IUsbCallback callback = android.hardware.usb.V1_0.IUsbCallback.asInterface(_hidl_request.readStrongBinder());
                    setCallback(callback);
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hardware.usb.V1_0.IUsb.kInterfaceName);
                    queryPortStatus();
                    return;
                case 4:
                    _hidl_request.enforceInterface(IUsb.kInterfaceName);
                    String portName2 = _hidl_request.readString();
                    boolean enable = _hidl_request.readBool();
                    enableContaminantPresenceDetection(portName2, enable);
                    return;
                case 5:
                    _hidl_request.enforceInterface(IUsb.kInterfaceName);
                    String portName3 = _hidl_request.readString();
                    boolean enable2 = _hidl_request.readBool();
                    enableContaminantPresenceProtection(portName3, enable2);
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
