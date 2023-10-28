package vendor.transsion.hardware.hyperion.hap.V1_0;

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
import com.android.server.usb.descriptors.UsbDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
/* loaded from: classes2.dex */
public interface IHap extends IBase {
    public static final String kInterfaceName = "vendor.transsion.hardware.hyperion.hap@1.0::IHap";

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface configureCallback {
        void onValues(int i, boolean z);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface decryptCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface encryptCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface enrollPasswordCallback {
        void onValues(int i, boolean z);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface exportKeyCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface getCryptoAliasesCallback {
        void onValues(int i, ArrayList<String> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface getImportKeyAADCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface getModuleInfoCallback {
        void onValues(int i, ModuleInfo moduleInfo);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface getStoreKeysCallback {
        void onValues(int i, ArrayList<String> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface getUsedSpaceCallback {
        void onValues(int i, int i2);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface isPasswordEnabledCallback {
        void onValues(int i, boolean z);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface loadByteArrayCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface loadByteArrayPersistCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface signCallback {
        void onValues(int i, ArrayList<Byte> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface verifyCallback {
        void onValues(int i, boolean z);
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface verifyPasswordCallback {
        void onValues(int i, boolean z);
    }

    @Override // android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    void configure(HyperionIdentity hyperionIdentity, String str, configureCallback configurecallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    void decrypt(HyperionIdentity hyperionIdentity, String str, ArrayList<Byte> arrayList, decryptCallback decryptcallback) throws RemoteException;

    void encrypt(HyperionIdentity hyperionIdentity, String str, ArrayList<Byte> arrayList, encryptCallback encryptcallback) throws RemoteException;

    void enrollPassword(HyperionIdentity hyperionIdentity, String str, String str2, enrollPasswordCallback enrollpasswordcallback) throws RemoteException;

    void exportKey(HyperionIdentity hyperionIdentity, String str, exportKeyCallback exportkeycallback) throws RemoteException;

    int generateKey(HyperionIdentity hyperionIdentity, String str, int i) throws RemoteException;

    void getCryptoAliases(HyperionIdentity hyperionIdentity, getCryptoAliasesCallback getcryptoaliasescallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getImportKeyAAD(int i, getImportKeyAADCallback getimportkeyaadcallback) throws RemoteException;

    void getModuleInfo(getModuleInfoCallback getmoduleinfocallback) throws RemoteException;

    void getStoreKeys(HyperionIdentity hyperionIdentity, int i, getStoreKeysCallback getstorekeyscallback) throws RemoteException;

    void getUsedSpace(HyperionIdentity hyperionIdentity, int i, getUsedSpaceCallback getusedspacecallback) throws RemoteException;

    void hello(String str) throws RemoteException;

    int importKey(HyperionIdentity hyperionIdentity, String str, int i, String str2, ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2, ArrayList<Byte> arrayList3, ArrayList<Byte> arrayList4, ArrayList<Byte> arrayList5) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    void invalidAccess(HyperionIdentity hyperionIdentity) throws RemoteException;

    void isPasswordEnabled(HyperionIdentity hyperionIdentity, isPasswordEnabledCallback ispasswordenabledcallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void loadByteArray(HyperionIdentity hyperionIdentity, String str, loadByteArrayCallback loadbytearraycallback) throws RemoteException;

    void loadByteArrayPersist(HyperionIdentity hyperionIdentity, String str, loadByteArrayPersistCallback loadbytearraypersistcallback) throws RemoteException;

    int loadFile(HyperionIdentity hyperionIdentity, String str, String str2) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    int removeKey(HyperionIdentity hyperionIdentity, String str) throws RemoteException;

    int removeStoreKey(HyperionIdentity hyperionIdentity, String str, int i) throws RemoteException;

    void resetApplication(HyperionIdentity hyperionIdentity, String str) throws RemoteException;

    void resetPassword(HyperionIdentity hyperionIdentity, String str) throws RemoteException;

    int saveByteArray(HyperionIdentity hyperionIdentity, String str, ArrayList<Byte> arrayList) throws RemoteException;

    int saveByteArrayPersist(HyperionIdentity hyperionIdentity, String str, ArrayList<Byte> arrayList) throws RemoteException;

    int saveFile(HyperionIdentity hyperionIdentity, String str, String str2) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    void sign(HyperionIdentity hyperionIdentity, String str, ArrayList<Byte> arrayList, signCallback signcallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    int updateKey(HyperionIdentity hyperionIdentity, String str, int i) throws RemoteException;

    void verify(HyperionIdentity hyperionIdentity, String str, ArrayList<Byte> arrayList, ArrayList<Byte> arrayList2, verifyCallback verifycallback) throws RemoteException;

    void verifyPassword(HyperionIdentity hyperionIdentity, String str, verifyPasswordCallback verifypasswordcallback) throws RemoteException;

    static IHap asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IHap)) {
            return (IHap) iface;
        }
        IHap proxy = new Proxy(binder);
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

    static IHap castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IHap getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IHap getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IHap getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IHap getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes2.dex */
    public static final class Proxy implements IHap {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.transsion.hardware.hyperion.hap@1.0::IHap]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void hello(String value) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            _hidl_request.writeString(value);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void getModuleInfo(getModuleInfoCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ModuleInfo _hidl_out_info = new ModuleInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_info);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void configure(HyperionIdentity id, String licence, configureCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(licence);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                boolean _hidl_out_res = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_res);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void resetApplication(HyperionIdentity id, String packageName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(packageName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void enrollPassword(HyperionIdentity id, String newPw, String oldPw, enrollPasswordCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(newPw);
            _hidl_request.writeString(oldPw);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                boolean _hidl_out_res = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_res);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void isPasswordEnabled(HyperionIdentity id, isPasswordEnabledCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                boolean _hidl_out_res = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_res);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void verifyPassword(HyperionIdentity id, String oldPw, verifyPasswordCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(oldPw);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                boolean _hidl_out_res = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_res);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void resetPassword(HyperionIdentity id, String oldPw) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(oldPw);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void invalidAccess(HyperionIdentity id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void getUsedSpace(HyperionIdentity id, int location, getUsedSpaceCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeInt32(location);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                int _hidl_out_used = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_used);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int saveByteArray(HyperionIdentity id, String key, ArrayList<Byte> value) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            _hidl_request.writeInt8Vector(value);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void loadByteArray(HyperionIdentity id, String key, loadByteArrayCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_content = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_content);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int saveByteArrayPersist(HyperionIdentity id, String key, ArrayList<Byte> value) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            _hidl_request.writeInt8Vector(value);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void loadByteArrayPersist(HyperionIdentity id, String key, loadByteArrayPersistCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(14, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_content = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_content);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int saveFile(HyperionIdentity id, String key, String fromPath) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            _hidl_request.writeString(fromPath);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(15, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int loadFile(HyperionIdentity id, String key, String toPath) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            _hidl_request.writeString(toPath);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(16, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int removeStoreKey(HyperionIdentity id, String key, int location) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(key);
            _hidl_request.writeInt32(location);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(17, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void getStoreKeys(HyperionIdentity id, int location, getStoreKeysCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeInt32(location);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(18, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<String> _hidl_out_keys = _hidl_reply.readStringVector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_keys);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int generateKey(HyperionIdentity id, String alias, int type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt32(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(19, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int updateKey(HyperionIdentity id, String alias, int type) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt32(type);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(20, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int removeKey(HyperionIdentity id, String alias) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(21, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void exportKey(HyperionIdentity id, String alias, exportKeyCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(22, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_keyMaterial = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_keyMaterial);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void getImportKeyAAD(int keyType, getImportKeyAADCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            _hidl_request.writeInt32(keyType);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(23, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_aad = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_aad);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public int importKey(HyperionIdentity id, String alias, int type, String decryptKeyAlias, ArrayList<Byte> encryptedTmpKey, ArrayList<Byte> encryptedKey, ArrayList<Byte> iv, ArrayList<Byte> aad, ArrayList<Byte> tag) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt32(type);
            _hidl_request.writeString(decryptKeyAlias);
            _hidl_request.writeInt8Vector(encryptedTmpKey);
            _hidl_request.writeInt8Vector(encryptedKey);
            _hidl_request.writeInt8Vector(iv);
            _hidl_request.writeInt8Vector(aad);
            _hidl_request.writeInt8Vector(tag);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(24, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                return _hidl_out_st;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void getCryptoAliases(HyperionIdentity id, getCryptoAliasesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(25, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<String> _hidl_out_aliases = _hidl_reply.readStringVector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_aliases);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void encrypt(HyperionIdentity id, String alias, ArrayList<Byte> input, encryptCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt8Vector(input);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(26, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_output = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_output);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void decrypt(HyperionIdentity id, String alias, ArrayList<Byte> input, decryptCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt8Vector(input);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(27, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_output = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_output);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void sign(HyperionIdentity id, String alias, ArrayList<Byte> input, signCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt8Vector(input);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(28, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                ArrayList<Byte> _hidl_out_output = _hidl_reply.readInt8Vector();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_output);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap
        public void verify(HyperionIdentity id, String alias, ArrayList<Byte> input, ArrayList<Byte> signature, verifyCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IHap.kInterfaceName);
            id.writeToParcel(_hidl_request);
            _hidl_request.writeString(alias);
            _hidl_request.writeInt8Vector(input);
            _hidl_request.writeInt8Vector(signature);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(29, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_st = _hidl_reply.readInt32();
                boolean _hidl_out_res = _hidl_reply.readBool();
                _hidl_cb.onValues(_hidl_out_st, _hidl_out_res);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
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

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class Stub extends HwBinder implements IHap {
        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IHap.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IHap.kInterfaceName;
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{1, -95, 40, -98, AcquiredInfo.MOUTH_COVERING_DETECTED, 38, 78, 62, 125, -30, -92, -93, 71, 61, 59, 38, 110, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, 100, -45, -97, -33, -72, 78, 9, 112, 78, 55, 122, 91, -16, -102}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IHap.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    String value = _hidl_request.readString();
                    hello(value);
                    return;
                case 2:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    getModuleInfo(new getModuleInfoCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.1
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.getModuleInfoCallback
                        public void onValues(int st, ModuleInfo info) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            info.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id = new HyperionIdentity();
                    id.readFromParcel(_hidl_request);
                    String licence = _hidl_request.readString();
                    configure(id, licence, new configureCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.2
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.configureCallback
                        public void onValues(int st, boolean res) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeBool(res);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id2 = new HyperionIdentity();
                    id2.readFromParcel(_hidl_request);
                    String packageName = _hidl_request.readString();
                    resetApplication(id2, packageName);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id3 = new HyperionIdentity();
                    id3.readFromParcel(_hidl_request);
                    String newPw = _hidl_request.readString();
                    String oldPw = _hidl_request.readString();
                    enrollPassword(id3, newPw, oldPw, new enrollPasswordCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.3
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.enrollPasswordCallback
                        public void onValues(int st, boolean res) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeBool(res);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 6:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id4 = new HyperionIdentity();
                    id4.readFromParcel(_hidl_request);
                    isPasswordEnabled(id4, new isPasswordEnabledCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.4
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.isPasswordEnabledCallback
                        public void onValues(int st, boolean res) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeBool(res);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 7:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id5 = new HyperionIdentity();
                    id5.readFromParcel(_hidl_request);
                    String oldPw2 = _hidl_request.readString();
                    verifyPassword(id5, oldPw2, new verifyPasswordCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.5
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.verifyPasswordCallback
                        public void onValues(int st, boolean res) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeBool(res);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 8:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id6 = new HyperionIdentity();
                    id6.readFromParcel(_hidl_request);
                    String oldPw3 = _hidl_request.readString();
                    resetPassword(id6, oldPw3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id7 = new HyperionIdentity();
                    id7.readFromParcel(_hidl_request);
                    invalidAccess(id7);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id8 = new HyperionIdentity();
                    id8.readFromParcel(_hidl_request);
                    int location = _hidl_request.readInt32();
                    getUsedSpace(id8, location, new getUsedSpaceCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.6
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.getUsedSpaceCallback
                        public void onValues(int st, int used) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt32(used);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 11:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id9 = new HyperionIdentity();
                    id9.readFromParcel(_hidl_request);
                    String key = _hidl_request.readString();
                    ArrayList<Byte> value2 = _hidl_request.readInt8Vector();
                    int _hidl_out_st = saveByteArray(id9, key, value2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id10 = new HyperionIdentity();
                    id10.readFromParcel(_hidl_request);
                    String key2 = _hidl_request.readString();
                    loadByteArray(id10, key2, new loadByteArrayCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.7
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.loadByteArrayCallback
                        public void onValues(int st, ArrayList<Byte> content) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(content);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 13:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id11 = new HyperionIdentity();
                    id11.readFromParcel(_hidl_request);
                    String key3 = _hidl_request.readString();
                    ArrayList<Byte> value3 = _hidl_request.readInt8Vector();
                    int _hidl_out_st2 = saveByteArrayPersist(id11, key3, value3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st2);
                    _hidl_reply.send();
                    return;
                case 14:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id12 = new HyperionIdentity();
                    id12.readFromParcel(_hidl_request);
                    String key4 = _hidl_request.readString();
                    loadByteArrayPersist(id12, key4, new loadByteArrayPersistCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.8
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.loadByteArrayPersistCallback
                        public void onValues(int st, ArrayList<Byte> content) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(content);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 15:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id13 = new HyperionIdentity();
                    id13.readFromParcel(_hidl_request);
                    String key5 = _hidl_request.readString();
                    String fromPath = _hidl_request.readString();
                    int _hidl_out_st3 = saveFile(id13, key5, fromPath);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st3);
                    _hidl_reply.send();
                    return;
                case 16:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id14 = new HyperionIdentity();
                    id14.readFromParcel(_hidl_request);
                    String key6 = _hidl_request.readString();
                    String toPath = _hidl_request.readString();
                    int _hidl_out_st4 = loadFile(id14, key6, toPath);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st4);
                    _hidl_reply.send();
                    return;
                case 17:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id15 = new HyperionIdentity();
                    id15.readFromParcel(_hidl_request);
                    String key7 = _hidl_request.readString();
                    int location2 = _hidl_request.readInt32();
                    int _hidl_out_st5 = removeStoreKey(id15, key7, location2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st5);
                    _hidl_reply.send();
                    return;
                case 18:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id16 = new HyperionIdentity();
                    id16.readFromParcel(_hidl_request);
                    int location3 = _hidl_request.readInt32();
                    getStoreKeys(id16, location3, new getStoreKeysCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.9
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.getStoreKeysCallback
                        public void onValues(int st, ArrayList<String> keys) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeStringVector(keys);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 19:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id17 = new HyperionIdentity();
                    id17.readFromParcel(_hidl_request);
                    String alias = _hidl_request.readString();
                    int type = _hidl_request.readInt32();
                    int _hidl_out_st6 = generateKey(id17, alias, type);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st6);
                    _hidl_reply.send();
                    return;
                case 20:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id18 = new HyperionIdentity();
                    id18.readFromParcel(_hidl_request);
                    String alias2 = _hidl_request.readString();
                    int type2 = _hidl_request.readInt32();
                    int _hidl_out_st7 = updateKey(id18, alias2, type2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st7);
                    _hidl_reply.send();
                    return;
                case 21:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id19 = new HyperionIdentity();
                    id19.readFromParcel(_hidl_request);
                    String alias3 = _hidl_request.readString();
                    int _hidl_out_st8 = removeKey(id19, alias3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st8);
                    _hidl_reply.send();
                    return;
                case 22:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id20 = new HyperionIdentity();
                    id20.readFromParcel(_hidl_request);
                    String alias4 = _hidl_request.readString();
                    exportKey(id20, alias4, new exportKeyCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.10
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.exportKeyCallback
                        public void onValues(int st, ArrayList<Byte> keyMaterial) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(keyMaterial);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 23:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    int keyType = _hidl_request.readInt32();
                    getImportKeyAAD(keyType, new getImportKeyAADCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.11
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.getImportKeyAADCallback
                        public void onValues(int st, ArrayList<Byte> aad) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(aad);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 24:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id21 = new HyperionIdentity();
                    id21.readFromParcel(_hidl_request);
                    String alias5 = _hidl_request.readString();
                    int type3 = _hidl_request.readInt32();
                    String decryptKeyAlias = _hidl_request.readString();
                    ArrayList<Byte> encryptedTmpKey = _hidl_request.readInt8Vector();
                    ArrayList<Byte> encryptedKey = _hidl_request.readInt8Vector();
                    ArrayList<Byte> iv = _hidl_request.readInt8Vector();
                    ArrayList<Byte> aad = _hidl_request.readInt8Vector();
                    ArrayList<Byte> tag = _hidl_request.readInt8Vector();
                    int _hidl_out_st9 = importKey(id21, alias5, type3, decryptKeyAlias, encryptedTmpKey, encryptedKey, iv, aad, tag);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_st9);
                    _hidl_reply.send();
                    return;
                case 25:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id22 = new HyperionIdentity();
                    id22.readFromParcel(_hidl_request);
                    getCryptoAliases(id22, new getCryptoAliasesCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.12
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.getCryptoAliasesCallback
                        public void onValues(int st, ArrayList<String> aliases) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeStringVector(aliases);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 26:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id23 = new HyperionIdentity();
                    id23.readFromParcel(_hidl_request);
                    String alias6 = _hidl_request.readString();
                    ArrayList<Byte> input = _hidl_request.readInt8Vector();
                    encrypt(id23, alias6, input, new encryptCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.13
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.encryptCallback
                        public void onValues(int st, ArrayList<Byte> output) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(output);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 27:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id24 = new HyperionIdentity();
                    id24.readFromParcel(_hidl_request);
                    String alias7 = _hidl_request.readString();
                    ArrayList<Byte> input2 = _hidl_request.readInt8Vector();
                    decrypt(id24, alias7, input2, new decryptCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.14
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.decryptCallback
                        public void onValues(int st, ArrayList<Byte> output) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(output);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 28:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id25 = new HyperionIdentity();
                    id25.readFromParcel(_hidl_request);
                    String alias8 = _hidl_request.readString();
                    ArrayList<Byte> input3 = _hidl_request.readInt8Vector();
                    sign(id25, alias8, input3, new signCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.15
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.signCallback
                        public void onValues(int st, ArrayList<Byte> output) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeInt8Vector(output);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 29:
                    _hidl_request.enforceInterface(IHap.kInterfaceName);
                    HyperionIdentity id26 = new HyperionIdentity();
                    id26.readFromParcel(_hidl_request);
                    String alias9 = _hidl_request.readString();
                    ArrayList<Byte> input4 = _hidl_request.readInt8Vector();
                    ArrayList<Byte> signature = _hidl_request.readInt8Vector();
                    verify(id26, alias9, input4, signature, new verifyCallback() { // from class: vendor.transsion.hardware.hyperion.hap.V1_0.IHap.Stub.16
                        @Override // vendor.transsion.hardware.hyperion.hap.V1_0.IHap.verifyCallback
                        public void onValues(int st, boolean res) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(st);
                            _hidl_reply.writeBool(res);
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
