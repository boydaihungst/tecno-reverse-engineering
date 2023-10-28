package vendor.mediatek.hardware.nvram.V1_1;

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
/* loaded from: classes2.dex */
public interface INvram extends vendor.mediatek.hardware.nvram.V1_0.INvram {
    public static final String kInterfaceName = "vendor.mediatek.hardware.nvram@1.1::INvram";

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface ReadFileVerInfoCallback {
        void onValues(int i, int i2, int i3);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface readFileBylidCallback {
        void onValues(String str, int i, byte b);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface writeFileBylidCallback {
        void onValues(int i, byte b);
    }

    boolean AddBackupFileNum(int i) throws RemoteException;

    boolean BackupAll_NvRam() throws RemoteException;

    boolean BackupData_Special(ArrayList<Byte> arrayList, int i, int i2) throws RemoteException;

    boolean BackupToBinRegion_All() throws RemoteException;

    boolean BackupToBinRegion_All_Ex(int i) throws RemoteException;

    boolean BackupToBinRegion_All_Exx(ArrayList<Byte> arrayList) throws RemoteException;

    boolean DeleteFile(String str) throws RemoteException;

    void ReadFileVerInfo(int i, ReadFileVerInfoCallback readFileVerInfoCallback) throws RemoteException;

    boolean ResetFileToDefault(int i) throws RemoteException;

    boolean RestoreAll_NvRam() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    void readFileBylid(int i, short s, readFileBylidCallback readfilebylidcallback) throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    void writeFileBylid(int i, short s, ArrayList<Byte> arrayList, writeFileBylidCallback writefilebylidcallback) throws RemoteException;

    static INvram asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof INvram)) {
            return (INvram) iface;
        }
        INvram proxy = new Proxy(binder);
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

    static INvram castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static INvram getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static INvram getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static INvram getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static INvram getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes2.dex */
    public static final class Proxy implements INvram {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.mediatek.hardware.nvram@1.1::INvram]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.mediatek.hardware.nvram.V1_0.INvram
        public String readFileByName(String filename, int size) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.mediatek.hardware.nvram.V1_0.INvram.kInterfaceName);
            _hidl_request.writeString(filename);
            _hidl_request.writeInt32(size);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_data = _hidl_reply.readString();
                return _hidl_out_data;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_0.INvram
        public byte writeFileByNamevec(String filename, int size, ArrayList<Byte> data) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(vendor.mediatek.hardware.nvram.V1_0.INvram.kInterfaceName);
            _hidl_request.writeString(filename);
            _hidl_request.writeInt32(size);
            _hidl_request.writeInt8Vector(data);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                byte _hidl_out_retval = _hidl_reply.readInt8();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public void readFileBylid(int lid, short para, readFileBylidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt32(lid);
            _hidl_request.writeInt16(para);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_data = _hidl_reply.readString();
                int _hidl_out_readsize = _hidl_reply.readInt32();
                byte _hidl_out_retval = _hidl_reply.readInt8();
                _hidl_cb.onValues(_hidl_out_data, _hidl_out_readsize, _hidl_out_retval);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public void writeFileBylid(int lid, short para, ArrayList<Byte> data, writeFileBylidCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt32(lid);
            _hidl_request.writeInt16(para);
            _hidl_request.writeInt8Vector(data);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_writesize = _hidl_reply.readInt32();
                byte _hidl_out_retval = _hidl_reply.readInt8();
                _hidl_cb.onValues(_hidl_out_writesize, _hidl_out_retval);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean AddBackupFileNum(int lid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt32(lid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean ResetFileToDefault(int lid) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt32(lid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public void ReadFileVerInfo(int lid, ReadFileVerInfoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt32(lid);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_i4RecSize = _hidl_reply.readInt32();
                int _hidl_out_i4RecNum = _hidl_reply.readInt32();
                int _hidl_out_i4MaxFileLid = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_i4RecSize, _hidl_out_i4RecNum, _hidl_out_i4MaxFileLid);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean BackupData_Special(ArrayList<Byte> data, int count, int mode) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt8Vector(data);
            _hidl_request.writeInt32(count);
            _hidl_request.writeInt32(mode);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean BackupAll_NvRam() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean RestoreAll_NvRam() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean BackupToBinRegion_All() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean BackupToBinRegion_All_Exx(ArrayList<Byte> data) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt8Vector(data);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean BackupToBinRegion_All_Ex(int value) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeInt32(value);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram
        public boolean DeleteFile(String filename) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(INvram.kInterfaceName);
            _hidl_request.writeString(filename);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_retval = _hidl_reply.readBool();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
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

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends HwBinder implements INvram {
        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(INvram.kInterfaceName, vendor.mediatek.hardware.nvram.V1_0.INvram.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return INvram.kInterfaceName;
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{80, -34, 4, -59, -117, -111, 79, Byte.MAX_VALUE, 52, 114, -96, -105, -33, -18, -6, Byte.MIN_VALUE, 96, 32, 104, -87, 86, -54, 28, -52, UsbASFormat.EXT_FORMAT_TYPE_III, 59, -88, 69, -58, -50, -98, 123}, new byte[]{UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -52, 54, -83, UsbASFormat.EXT_FORMAT_TYPE_III, AcquiredInfo.MOUTH_COVERING_DETECTED, 0, -62, -12, -116, -87, -6, 67, -11, 28, 58, -66, 77, 113, 116, -84, 62, 6, -53, 82, -120, -80, -8, -13, -62, -4, 0}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram, vendor.mediatek.hardware.nvram.V1_0.INvram, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (INvram.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(vendor.mediatek.hardware.nvram.V1_0.INvram.kInterfaceName);
                    String filename = _hidl_request.readString();
                    int size = _hidl_request.readInt32();
                    String _hidl_out_data = readFileByName(filename, size);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_data);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(vendor.mediatek.hardware.nvram.V1_0.INvram.kInterfaceName);
                    String filename2 = _hidl_request.readString();
                    int size2 = _hidl_request.readInt32();
                    ArrayList<Byte> data = _hidl_request.readInt8Vector();
                    byte _hidl_out_retval = writeFileByNamevec(filename2, size2, data);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt8(_hidl_out_retval);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    int lid = _hidl_request.readInt32();
                    short para = _hidl_request.readInt16();
                    readFileBylid(lid, para, new readFileBylidCallback() { // from class: vendor.mediatek.hardware.nvram.V1_1.INvram.Stub.1
                        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram.readFileBylidCallback
                        public void onValues(String data2, int readsize, byte retval) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeString(data2);
                            _hidl_reply.writeInt32(readsize);
                            _hidl_reply.writeInt8(retval);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    int lid2 = _hidl_request.readInt32();
                    short para2 = _hidl_request.readInt16();
                    ArrayList<Byte> data2 = _hidl_request.readInt8Vector();
                    writeFileBylid(lid2, para2, data2, new writeFileBylidCallback() { // from class: vendor.mediatek.hardware.nvram.V1_1.INvram.Stub.2
                        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram.writeFileBylidCallback
                        public void onValues(int writesize, byte retval) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(writesize);
                            _hidl_reply.writeInt8(retval);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 5:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    int lid3 = _hidl_request.readInt32();
                    boolean _hidl_out_retval2 = AddBackupFileNum(lid3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval2);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    int lid4 = _hidl_request.readInt32();
                    boolean _hidl_out_retval3 = ResetFileToDefault(lid4);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval3);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    int lid5 = _hidl_request.readInt32();
                    ReadFileVerInfo(lid5, new ReadFileVerInfoCallback() { // from class: vendor.mediatek.hardware.nvram.V1_1.INvram.Stub.3
                        @Override // vendor.mediatek.hardware.nvram.V1_1.INvram.ReadFileVerInfoCallback
                        public void onValues(int i4RecSize, int i4RecNum, int i4MaxFileLid) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(i4RecSize);
                            _hidl_reply.writeInt32(i4RecNum);
                            _hidl_reply.writeInt32(i4MaxFileLid);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 8:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    ArrayList<Byte> data3 = _hidl_request.readInt8Vector();
                    int count = _hidl_request.readInt32();
                    int mode = _hidl_request.readInt32();
                    boolean _hidl_out_retval4 = BackupData_Special(data3, count, mode);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval4);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    boolean _hidl_out_retval5 = BackupAll_NvRam();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval5);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    boolean _hidl_out_retval6 = RestoreAll_NvRam();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval6);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    boolean _hidl_out_retval7 = BackupToBinRegion_All();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval7);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    ArrayList<Byte> data4 = _hidl_request.readInt8Vector();
                    boolean _hidl_out_retval8 = BackupToBinRegion_All_Exx(data4);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval8);
                    _hidl_reply.send();
                    return;
                case 13:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    int value = _hidl_request.readInt32();
                    boolean _hidl_out_retval9 = BackupToBinRegion_All_Ex(value);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval9);
                    _hidl_reply.send();
                    return;
                case 14:
                    _hidl_request.enforceInterface(INvram.kInterfaceName);
                    String filename3 = _hidl_request.readString();
                    boolean _hidl_out_retval10 = DeleteFile(filename3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_retval10);
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