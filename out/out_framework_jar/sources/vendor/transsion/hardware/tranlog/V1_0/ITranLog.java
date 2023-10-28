package vendor.transsion.hardware.tranlog.V1_0;

import android.internal.hidl.base.V1_0.DebugInfo;
import android.internal.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.internal.midi.MidiConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
/* loaded from: classes4.dex */
public interface ITranLog extends IBase {
    public static final String kInterfaceName = "vendor.transsion.hardware.tranlog@1.0::ITranLog";

    @Override // android.internal.hidl.base.V1_0.IBase, android.os.IHwInterface
    IHwBinder asBinder();

    @Override // android.internal.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    float getNvFloat(String str, float f) throws RemoteException;

    int getNvS32(String str, int i) throws RemoteException;

    long getNvS64(String str, long j) throws RemoteException;

    String getNvString(String str, String str2) throws RemoteException;

    void hello(int i) throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    void setIndication(ITranLogIndication iTranLogIndication) throws RemoteException;

    @Override // android.internal.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ITranLog asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ITranLog)) {
            return (ITranLog) iface;
        }
        ITranLog proxy = new Proxy(binder);
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

    static ITranLog castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ITranLog getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ITranLog getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    @Deprecated
    static ITranLog getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static ITranLog getService() throws RemoteException {
        return getService("default");
    }

    /* loaded from: classes4.dex */
    public static final class Proxy implements ITranLog {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase, android.os.IHwInterface
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.transsion.hardware.tranlog@1.0::ITranLog]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog
        public void hello(int value) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranLog.kInterfaceName);
            _hidl_request.writeInt32(value);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog
        public void setIndication(ITranLogIndication TranLogIndicationParam) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranLog.kInterfaceName);
            _hidl_request.writeStrongBinder(TranLogIndicationParam == null ? null : TranLogIndicationParam.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog
        public int getNvS32(String key, int def) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranLog.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeInt32(def);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_value = _hidl_reply.readInt32();
                return _hidl_out_value;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog
        public long getNvS64(String key, long def) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranLog.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeInt64(def);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                long _hidl_out_value = _hidl_reply.readInt64();
                return _hidl_out_value;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog
        public float getNvFloat(String key, float def) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranLog.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeFloat(def);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                float _hidl_out_value = _hidl_reply.readFloat();
                return _hidl_out_value;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog
        public String getNvString(String key, String def) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ITranLog.kInterfaceName);
            _hidl_request.writeString(key);
            _hidl_request.writeString(def);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_value = _hidl_reply.readString();
                return _hidl_out_value;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes4.dex */
    public static abstract class Stub extends HwBinder implements ITranLog {
        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase, android.os.IHwInterface
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ITranLog.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return ITranLog.kInterfaceName;
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-30, 14, -61, 42, 78, -53, 11, 0, 84, 60, -83, 79, -65, 74, 72, 99, -98, 84, -61, -26, -23, 120, -8, 8, 57, 54, -34, 12, -12, -118, 65, 98}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, MidiConstants.STATUS_CHANNEL_PRESSURE, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, MidiConstants.STATUS_SONG_SELECT, -51, 105, 87, 19, -109, 36, -72, 59, 24, -54, 76}));
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.os.IHwBinder, android.hardware.cas.V1_0.ICas, android.internal.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.transsion.hardware.tranlog.V1_0.ITranLog, android.internal.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.os.IHwBinder, android.hardware.cas.V1_0.ICas, android.internal.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        @Override // android.os.IHwBinder
        public IHwInterface queryLocalInterface(String descriptor) {
            if (ITranLog.kInterfaceName.equals(descriptor)) {
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

        @Override // android.os.HwBinder
        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(ITranLog.kInterfaceName);
                    int value = _hidl_request.readInt32();
                    hello(value);
                    return;
                case 2:
                    _hidl_request.enforceInterface(ITranLog.kInterfaceName);
                    ITranLogIndication TranLogIndicationParam = ITranLogIndication.asInterface(_hidl_request.readStrongBinder());
                    setIndication(TranLogIndicationParam);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(ITranLog.kInterfaceName);
                    String key = _hidl_request.readString();
                    int def = _hidl_request.readInt32();
                    int _hidl_out_value = getNvS32(key, def);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_value);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(ITranLog.kInterfaceName);
                    String key2 = _hidl_request.readString();
                    long def2 = _hidl_request.readInt64();
                    long _hidl_out_value2 = getNvS64(key2, def2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt64(_hidl_out_value2);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(ITranLog.kInterfaceName);
                    String key3 = _hidl_request.readString();
                    float def3 = _hidl_request.readFloat();
                    float _hidl_out_value3 = getNvFloat(key3, def3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeFloat(_hidl_out_value3);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(ITranLog.kInterfaceName);
                    String key4 = _hidl_request.readString();
                    String def4 = _hidl_request.readString();
                    String _hidl_out_value4 = getNvString(key4, def4);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_value4);
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
