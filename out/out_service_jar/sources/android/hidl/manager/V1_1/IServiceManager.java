package android.hidl.manager.V1_1;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
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
/* loaded from: classes.dex */
public interface IServiceManager extends android.hidl.manager.V1_0.IServiceManager {
    public static final String kInterfaceName = "android.hidl.manager@1.1::IServiceManager";

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    boolean unregisterForNotifications(String str, String str2, IServiceNotification iServiceNotification) throws RemoteException;

    static IServiceManager asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IServiceManager)) {
            return (IServiceManager) iface;
        }
        IServiceManager proxy = new Proxy(binder);
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

    static IServiceManager castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IServiceManager getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IServiceManager getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IServiceManager getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IServiceManager getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IServiceManager {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hidl.manager@1.1::IServiceManager]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public IBase get(String fqName, String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            _hidl_request.writeString(fqName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IBase _hidl_out_service = IBase.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_service;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public boolean add(String name, IBase service) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            _hidl_request.writeString(name);
            _hidl_request.writeStrongBinder(service == null ? null : service.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_success = _hidl_reply.readBool();
                return _hidl_out_success;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public byte getTransport(String fqName, String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            _hidl_request.writeString(fqName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                byte _hidl_out_transport = _hidl_reply.readInt8();
                return _hidl_out_transport;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public ArrayList<String> list() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_fqInstanceNames = _hidl_reply.readStringVector();
                return _hidl_out_fqInstanceNames;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public ArrayList<String> listByInterface(String fqName) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            _hidl_request.writeString(fqName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_instanceNames = _hidl_reply.readStringVector();
                return _hidl_out_instanceNames;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public boolean registerForNotifications(String fqName, String name, IServiceNotification callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            _hidl_request.writeString(fqName);
            _hidl_request.writeString(name);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_success = _hidl_reply.readBool();
                return _hidl_out_success;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public ArrayList<IServiceManager.InstanceDebugInfo> debugDump() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<IServiceManager.InstanceDebugInfo> _hidl_out_info = IServiceManager.InstanceDebugInfo.readVectorFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_0.IServiceManager
        public void registerPassthroughClient(String fqName, String name) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
            _hidl_request.writeString(fqName);
            _hidl_request.writeString(name);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_1.IServiceManager
        public boolean unregisterForNotifications(String fqName, String name, IServiceNotification callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IServiceManager.kInterfaceName);
            _hidl_request.writeString(fqName);
            _hidl_request.writeString(name);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_success = _hidl_reply.readBool();
                return _hidl_out_success;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
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

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IServiceManager {
        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IServiceManager.kInterfaceName, android.hidl.manager.V1_0.IServiceManager.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IServiceManager.kInterfaceName;
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{11, -108, -36, -121, 111, 116, -98, -46, 74, -104, -10, 28, 65, -44, 106, -41, 90, 39, 81, AcquiredInfo.PAN_TOO_EXTREME, 99, -15, -106, -118, 8, 66, AcquiredInfo.ROLL_TOO_EXTREME, -93, 60, 104, 78, -10}, new byte[]{-123, 57, 79, -118, 13, AcquiredInfo.START, -25, -5, 46, -28, 92, 82, -47, -5, -117, -113, -45, -63, 60, 51, 62, 99, -57, -116, 74, -95, -1, -122, -124, 12, -10, -36}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hidl.manager.V1_1.IServiceManager, android.hidl.manager.V1_0.IServiceManager, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IServiceManager.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    String fqName = _hidl_request.readString();
                    String name = _hidl_request.readString();
                    IBase _hidl_out_service = get(fqName, name);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_service == null ? null : _hidl_out_service.asBinder());
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    String name2 = _hidl_request.readString();
                    IBase service = IBase.asInterface(_hidl_request.readStrongBinder());
                    boolean _hidl_out_success = add(name2, service);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_success);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    String fqName2 = _hidl_request.readString();
                    String name3 = _hidl_request.readString();
                    byte _hidl_out_transport = getTransport(fqName2, name3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt8(_hidl_out_transport);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    ArrayList<String> _hidl_out_fqInstanceNames = list();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStringVector(_hidl_out_fqInstanceNames);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    String fqName3 = _hidl_request.readString();
                    ArrayList<String> _hidl_out_instanceNames = listByInterface(fqName3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStringVector(_hidl_out_instanceNames);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    String fqName4 = _hidl_request.readString();
                    String name4 = _hidl_request.readString();
                    IServiceNotification callback = IServiceNotification.asInterface(_hidl_request.readStrongBinder());
                    boolean _hidl_out_success2 = registerForNotifications(fqName4, name4, callback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_success2);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    ArrayList<IServiceManager.InstanceDebugInfo> _hidl_out_info = debugDump();
                    _hidl_reply.writeStatus(0);
                    IServiceManager.InstanceDebugInfo.writeVectorToParcel(_hidl_reply, _hidl_out_info);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(android.hidl.manager.V1_0.IServiceManager.kInterfaceName);
                    String fqName5 = _hidl_request.readString();
                    String name5 = _hidl_request.readString();
                    registerPassthroughClient(fqName5, name5);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(IServiceManager.kInterfaceName);
                    String fqName6 = _hidl_request.readString();
                    String name6 = _hidl_request.readString();
                    IServiceNotification callback2 = IServiceNotification.asInterface(_hidl_request.readStrongBinder());
                    boolean _hidl_out_success3 = unregisterForNotifications(fqName6, name6, callback2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_success3);
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
                    DebugInfo _hidl_out_info2 = getDebugInfo();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_info2.writeToParcel(_hidl_reply);
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
