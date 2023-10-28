package android.hardware.health.V2_1;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hardware.health.V2_0.DiskStats;
import android.hardware.health.V2_0.IHealth;
import android.hardware.health.V2_0.StorageInfo;
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
public interface IHealth extends android.hardware.health.V2_0.IHealth {
    public static final String kInterfaceName = "android.hardware.health@2.1::IHealth";

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface getHealthConfigCallback {
        void onValues(int i, HealthConfig healthConfig);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface getHealthInfo_2_1Callback {
        void onValues(int i, HealthInfo healthInfo);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface shouldKeepScreenOnCallback {
        void onValues(int i, boolean z);
    }

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getHealthConfig(getHealthConfigCallback gethealthconfigcallback) throws RemoteException;

    void getHealthInfo_2_1(getHealthInfo_2_1Callback gethealthinfo_2_1callback) throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    void shouldKeepScreenOn(shouldKeepScreenOnCallback shouldkeepscreenoncallback) throws RemoteException;

    @Override // android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IHealth asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IHealth)) {
            return (IHealth) iface;
        }
        IHealth proxy = new Proxy(binder);
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

    static IHealth castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IHealth getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IHealth getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IHealth getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IHealth getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IHealth {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.health@2.1::IHealth]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.health.V2_0.IHealth
        public int registerCallback(android.hardware.health.V2_0.IHealthInfoCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
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

        @Override // android.hardware.health.V2_0.IHealth
        public int unregisterCallback(android.hardware.health.V2_0.IHealthInfoCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public int update() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getChargeCounter(IHealth.getChargeCounterCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getCurrentNow(IHealth.getCurrentNowCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getCurrentAverage(IHealth.getCurrentAverageCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getCapacity(IHealth.getCapacityCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getEnergyCounter(IHealth.getEnergyCounterCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                long _hidl_out_value = _hidl_reply.readInt64();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getChargeStatus(IHealth.getChargeStatusCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getStorageInfo(IHealth.getStorageInfoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                ArrayList<StorageInfo> _hidl_out_value = StorageInfo.readVectorFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getDiskStats(IHealth.getDiskStatsCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                ArrayList<DiskStats> _hidl_out_value = DiskStats.readVectorFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_0.IHealth
        public void getHealthInfo(IHealth.getHealthInfoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.health.V2_0.IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                android.hardware.health.V2_0.HealthInfo _hidl_out_value = new android.hardware.health.V2_0.HealthInfo();
                _hidl_out_value.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_1.IHealth
        public void getHealthConfig(getHealthConfigCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                HealthConfig _hidl_out_config = new HealthConfig();
                _hidl_out_config.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_config);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_1.IHealth
        public void getHealthInfo_2_1(getHealthInfo_2_1Callback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                HealthInfo _hidl_out_value = new HealthInfo();
                _hidl_out_value.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_1.IHealth
        public void shouldKeepScreenOn(shouldKeepScreenOnCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHealth.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                boolean _hidl_out_value = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IHealth {
        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IHealth.kInterfaceName, android.hardware.health.V2_0.IHealth.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IHealth.kInterfaceName;
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-50, -115, -66, 118, -21, -98, -23, 75, 70, -17, -104, -9, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_ENDPOINT, -66, -103, 46, 118, 10, 87, 81, 7, 61, 79, 73, AcquiredInfo.TILT_TOO_EXTREME, 72, 64, 38, 84, AcquiredInfo.ROLL_TOO_EXTREME, 113, -13}, new byte[]{103, 86, UsbASFormat.EXT_FORMAT_TYPE_II, -35, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, 7, Byte.MIN_VALUE, 92, -104, 94, -86, -20, -111, 97, UsbDescriptor.DESCRIPTORTYPE_SUPERSPEED_HUB, -68, -120, -12, -62, 91, 52, 49, -5, -124, 7, 11, 117, -124, -95, -89, 65, -5}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.health.V2_1.IHealth, android.hardware.health.V2_0.IHealth, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IHealth.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    android.hardware.health.V2_0.IHealthInfoCallback callback = android.hardware.health.V2_0.IHealthInfoCallback.asInterface(_hidl_request.readStrongBinder());
                    int _hidl_out_result = registerCallback(callback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    android.hardware.health.V2_0.IHealthInfoCallback callback2 = android.hardware.health.V2_0.IHealthInfoCallback.asInterface(_hidl_request.readStrongBinder());
                    int _hidl_out_result2 = unregisterCallback(callback2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result2);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    int _hidl_out_result3 = update();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result3);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getChargeCounter(new IHealth.getChargeCounterCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.1
                        @Override // android.hardware.health.V2_0.IHealth.getChargeCounterCallback
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 5:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getCurrentNow(new IHealth.getCurrentNowCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.2
                        @Override // android.hardware.health.V2_0.IHealth.getCurrentNowCallback
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 6:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getCurrentAverage(new IHealth.getCurrentAverageCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.3
                        @Override // android.hardware.health.V2_0.IHealth.getCurrentAverageCallback
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 7:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getCapacity(new IHealth.getCapacityCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.4
                        @Override // android.hardware.health.V2_0.IHealth.getCapacityCallback
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 8:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getEnergyCounter(new IHealth.getEnergyCounterCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.5
                        @Override // android.hardware.health.V2_0.IHealth.getEnergyCounterCallback
                        public void onValues(int result, long value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt64(value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 9:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getChargeStatus(new IHealth.getChargeStatusCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.6
                        @Override // android.hardware.health.V2_0.IHealth.getChargeStatusCallback
                        public void onValues(int result, int value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 10:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getStorageInfo(new IHealth.getStorageInfoCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.7
                        @Override // android.hardware.health.V2_0.IHealth.getStorageInfoCallback
                        public void onValues(int result, ArrayList<StorageInfo> value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            StorageInfo.writeVectorToParcel(_hidl_reply, value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 11:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getDiskStats(new IHealth.getDiskStatsCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.8
                        @Override // android.hardware.health.V2_0.IHealth.getDiskStatsCallback
                        public void onValues(int result, ArrayList<DiskStats> value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            DiskStats.writeVectorToParcel(_hidl_reply, value);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 12:
                    _hidl_request.enforceInterface(android.hardware.health.V2_0.IHealth.kInterfaceName);
                    getHealthInfo(new IHealth.getHealthInfoCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.9
                        @Override // android.hardware.health.V2_0.IHealth.getHealthInfoCallback
                        public void onValues(int result, android.hardware.health.V2_0.HealthInfo value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            value.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 13:
                    _hidl_request.enforceInterface(IHealth.kInterfaceName);
                    getHealthConfig(new getHealthConfigCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.10
                        @Override // android.hardware.health.V2_1.IHealth.getHealthConfigCallback
                        public void onValues(int result, HealthConfig config) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            config.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 14:
                    _hidl_request.enforceInterface(IHealth.kInterfaceName);
                    getHealthInfo_2_1(new getHealthInfo_2_1Callback() { // from class: android.hardware.health.V2_1.IHealth.Stub.11
                        @Override // android.hardware.health.V2_1.IHealth.getHealthInfo_2_1Callback
                        public void onValues(int result, HealthInfo value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            value.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 15:
                    _hidl_request.enforceInterface(IHealth.kInterfaceName);
                    shouldKeepScreenOn(new shouldKeepScreenOnCallback() { // from class: android.hardware.health.V2_1.IHealth.Stub.12
                        @Override // android.hardware.health.V2_1.IHealth.shouldKeepScreenOnCallback
                        public void onValues(int result, boolean value) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeBool(value);
                            _hidl_reply.send();
                        }
                    });
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
