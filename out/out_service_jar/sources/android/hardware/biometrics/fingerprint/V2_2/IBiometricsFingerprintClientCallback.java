package android.hardware.biometrics.fingerprint.V2_2;

import android.hardware.biometrics.face.AcquiredInfo;
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
public interface IBiometricsFingerprintClientCallback extends android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback {
    public static final String kInterfaceName = "android.hardware.biometrics.fingerprint@2.2::IBiometricsFingerprintClientCallback";

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    void onAcquired_2_2(long j, int i, int i2) throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IBiometricsFingerprintClientCallback asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IBiometricsFingerprintClientCallback)) {
            return (IBiometricsFingerprintClientCallback) iface;
        }
        IBiometricsFingerprintClientCallback proxy = new Proxy(binder);
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

    static IBiometricsFingerprintClientCallback castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IBiometricsFingerprintClientCallback getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IBiometricsFingerprintClientCallback getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IBiometricsFingerprintClientCallback getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IBiometricsFingerprintClientCallback getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IBiometricsFingerprintClientCallback {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.biometrics.fingerprint@2.2::IBiometricsFingerprintClientCallback]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onEnrollResult(long deviceId, int fingerId, int groupId, int remaining) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(fingerId);
            _hidl_request.writeInt32(groupId);
            _hidl_request.writeInt32(remaining);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onAcquired(long deviceId, int acquiredInfo, int vendorCode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(acquiredInfo);
            _hidl_request.writeInt32(vendorCode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onAuthenticated(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(fingerId);
            _hidl_request.writeInt32(groupId);
            _hidl_request.writeInt8Vector(token);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onError(long deviceId, int error, int vendorCode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(error);
            _hidl_request.writeInt32(vendorCode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onRemoved(long deviceId, int fingerId, int groupId, int remaining) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(fingerId);
            _hidl_request.writeInt32(groupId);
            _hidl_request.writeInt32(remaining);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onEnumerate(long deviceId, int fingerId, int groupId, int remaining) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(fingerId);
            _hidl_request.writeInt32(groupId);
            _hidl_request.writeInt32(remaining);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback
        public void onAcquired_2_2(long deviceId, int acquiredInfo, int vendorCode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprintClientCallback.kInterfaceName);
            _hidl_request.writeInt64(deviceId);
            _hidl_request.writeInt32(acquiredInfo);
            _hidl_request.writeInt32(vendorCode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IBiometricsFingerprintClientCallback {
        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IBiometricsFingerprintClientCallback.kInterfaceName, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IBiometricsFingerprintClientCallback.kInterfaceName;
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{UsbASFormat.EXT_FORMAT_TYPE_II, -54, -39, -97, 95, -21, 46, -87, -68, -44, 87, -112, 85, -19, -12, -81, -113, -21, -97, -58, 2, -90, -28, UsbASFormat.EXT_FORMAT_TYPE_II, 125, -35, 114, 125, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_ENDPOINT, 77, 73, -111}, new byte[]{-86, -69, 92, 60, 88, 85, -110, -41, 30, -27, 123, 119, UsbDescriptor.DESCRIPTORTYPE_HUB, -116, AcquiredInfo.FACE_OBSCURED, -103, 61, 119, -111, 77, -34, -86, 100, -78, -59, 16, -102, 96, 43, 2, -22, 71}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.biometrics.fingerprint.V2_2.IBiometricsFingerprintClientCallback, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IBiometricsFingerprintClientCallback.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId = _hidl_request.readInt64();
                    int fingerId = _hidl_request.readInt32();
                    int groupId = _hidl_request.readInt32();
                    int remaining = _hidl_request.readInt32();
                    onEnrollResult(deviceId, fingerId, groupId, remaining);
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId2 = _hidl_request.readInt64();
                    int acquiredInfo = _hidl_request.readInt32();
                    int vendorCode = _hidl_request.readInt32();
                    onAcquired(deviceId2, acquiredInfo, vendorCode);
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId3 = _hidl_request.readInt64();
                    int fingerId2 = _hidl_request.readInt32();
                    int groupId2 = _hidl_request.readInt32();
                    ArrayList<Byte> token = _hidl_request.readInt8Vector();
                    onAuthenticated(deviceId3, fingerId2, groupId2, token);
                    return;
                case 4:
                    _hidl_request.enforceInterface(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId4 = _hidl_request.readInt64();
                    int error = _hidl_request.readInt32();
                    int vendorCode2 = _hidl_request.readInt32();
                    onError(deviceId4, error, vendorCode2);
                    return;
                case 5:
                    _hidl_request.enforceInterface(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId5 = _hidl_request.readInt64();
                    int fingerId3 = _hidl_request.readInt32();
                    int groupId3 = _hidl_request.readInt32();
                    int remaining2 = _hidl_request.readInt32();
                    onRemoved(deviceId5, fingerId3, groupId3, remaining2);
                    return;
                case 6:
                    _hidl_request.enforceInterface(android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId6 = _hidl_request.readInt64();
                    int fingerId4 = _hidl_request.readInt32();
                    int groupId4 = _hidl_request.readInt32();
                    int remaining3 = _hidl_request.readInt32();
                    onEnumerate(deviceId6, fingerId4, groupId4, remaining3);
                    return;
                case 7:
                    _hidl_request.enforceInterface(IBiometricsFingerprintClientCallback.kInterfaceName);
                    long deviceId7 = _hidl_request.readInt64();
                    int acquiredInfo2 = _hidl_request.readInt32();
                    int vendorCode3 = _hidl_request.readInt32();
                    onAcquired_2_2(deviceId7, acquiredInfo2, vendorCode3);
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
