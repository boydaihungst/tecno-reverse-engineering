package vendor.mediatek.hardware.biometrics.fingerprint.V2_1;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback;
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
/* loaded from: classes2.dex */
public interface ITranBiometricsFingerprint extends IBiometricsFingerprint {
    public static final String kInterfaceName = "vendor.mediatek.hardware.biometrics.fingerprint@2.1::ITranBiometricsFingerprint";

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    int sendCommand(int i, int i2) throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ITranBiometricsFingerprint asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ITranBiometricsFingerprint)) {
            return (ITranBiometricsFingerprint) iface;
        }
        ITranBiometricsFingerprint proxy = new Proxy(binder);
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

    static ITranBiometricsFingerprint castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ITranBiometricsFingerprint getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ITranBiometricsFingerprint getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static ITranBiometricsFingerprint getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static ITranBiometricsFingerprint getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes2.dex */
    public static final class Proxy implements ITranBiometricsFingerprint {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.mediatek.hardware.biometrics.fingerprint@2.1::ITranBiometricsFingerprint]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public long setNotify(IBiometricsFingerprintClientCallback clientCallback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            _hidl_request.writeStrongBinder(clientCallback == null ? null : clientCallback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                long _hidl_out_deviceId = _hidl_reply.readInt64();
                return _hidl_out_deviceId;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public long preEnroll() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                long _hidl_out_authChallenge = _hidl_reply.readInt64();
                return _hidl_out_authChallenge;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int enroll(byte[] hat, int gid, int timeoutSec) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            HwBlob _hidl_blob = new HwBlob(69);
            if (hat == null || hat.length != 69) {
                throw new IllegalArgumentException("Array element is not of the expected length");
            }
            _hidl_blob.putInt8Array(0L, hat);
            _hidl_request.writeBuffer(_hidl_blob);
            _hidl_request.writeInt32(gid);
            _hidl_request.writeInt32(timeoutSec);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int postEnroll() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public long getAuthenticatorId() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                long _hidl_out_AuthenticatorId = _hidl_reply.readInt64();
                return _hidl_out_AuthenticatorId;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int cancel() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int enumerate() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int remove(int gid, int fid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            _hidl_request.writeInt32(gid);
            _hidl_request.writeInt32(fid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int setActiveGroup(int gid, String storePath) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            _hidl_request.writeInt32(gid);
            _hidl_request.writeString(storePath);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
        public int authenticate(long operationId, int gid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBiometricsFingerprint.kInterfaceName);
            _hidl_request.writeInt64(operationId);
            _hidl_request.writeInt32(gid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_debugErrno = _hidl_reply.readInt32();
                return _hidl_out_debugErrno;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint
        public int sendCommand(int command, int extras) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranBiometricsFingerprint.kInterfaceName);
            _hidl_request.writeInt32(command);
            _hidl_request.writeInt32(extras);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends HwBinder implements ITranBiometricsFingerprint {
        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ITranBiometricsFingerprint.kInterfaceName, IBiometricsFingerprint.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return ITranBiometricsFingerprint.kInterfaceName;
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{UsbDescriptor.DESCRIPTORTYPE_REPORT, 83, 3, -10, -66, 60, -93, 56, 83, -39, -29, 57, UsbASFormat.EXT_FORMAT_TYPE_I, -29, -45, 68, 125, 111, 107, -72, -26, -36, 13, -25, 91, 122, 9, 88, 117, -94, 112, 94}, new byte[]{31, -67, -63, -8, 82, -8, -67, 46, 74, 108, 92, -77, 10, -62, -73, -122, 104, -55, -115, -50, AcquiredInfo.PAN_TOO_EXTREME, -118, 97, 118, 45, 64, 52, -82, -123, -97, 67, -40}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.mediatek.hardware.biometrics.fingerprint.V2_1.ITranBiometricsFingerprint, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ITranBiometricsFingerprint.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    IBiometricsFingerprintClientCallback clientCallback = IBiometricsFingerprintClientCallback.asInterface(_hidl_request.readStrongBinder());
                    long _hidl_out_deviceId = setNotify(clientCallback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt64(_hidl_out_deviceId);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    long _hidl_out_authChallenge = preEnroll();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt64(_hidl_out_authChallenge);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    byte[] hat = new byte[69];
                    _hidl_request.readBuffer(69L).copyToInt8Array(0L, hat, 69);
                    int gid = _hidl_request.readInt32();
                    int timeoutSec = _hidl_request.readInt32();
                    int _hidl_out_debugErrno = enroll(hat, gid, timeoutSec);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    int _hidl_out_debugErrno2 = postEnroll();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno2);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    long _hidl_out_AuthenticatorId = getAuthenticatorId();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt64(_hidl_out_AuthenticatorId);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    int _hidl_out_debugErrno3 = cancel();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno3);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    int _hidl_out_debugErrno4 = enumerate();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno4);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    int gid2 = _hidl_request.readInt32();
                    int fid = _hidl_request.readInt32();
                    int _hidl_out_debugErrno5 = remove(gid2, fid);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno5);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    int gid3 = _hidl_request.readInt32();
                    String storePath = _hidl_request.readString();
                    int _hidl_out_debugErrno6 = setActiveGroup(gid3, storePath);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno6);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(IBiometricsFingerprint.kInterfaceName);
                    long operationId = _hidl_request.readInt64();
                    int gid4 = _hidl_request.readInt32();
                    int _hidl_out_debugErrno7 = authenticate(operationId, gid4);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_debugErrno7);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(ITranBiometricsFingerprint.kInterfaceName);
                    int command = _hidl_request.readInt32();
                    int extras = _hidl_request.readInt32();
                    int _hidl_out_ret = sendCommand(command, extras);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret);
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
