package android.hardware.broadcastradio.V2_0;

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
/* loaded from: classes.dex */
public interface IBroadcastRadio extends IBase {
    public static final String kInterfaceName = "android.hardware.broadcastradio@2.0::IBroadcastRadio";

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface getAmFmRegionConfigCallback {
        void onValues(int i, AmFmRegionConfig amFmRegionConfig);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface getDabRegionConfigCallback {
        void onValues(int i, ArrayList<DabTableEntry> arrayList);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface openSessionCallback {
        void onValues(int i, ITunerSession iTunerSession);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface registerAnnouncementListenerCallback {
        void onValues(int i, ICloseHandle iCloseHandle);
    }

    @Override // android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    void getAmFmRegionConfig(boolean z, getAmFmRegionConfigCallback getamfmregionconfigcallback) throws RemoteException;

    void getDabRegionConfig(getDabRegionConfigCallback getdabregionconfigcallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    ArrayList<Byte> getImage(int i) throws RemoteException;

    Properties getProperties() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    void openSession(ITunerCallback iTunerCallback, openSessionCallback opensessioncallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    void registerAnnouncementListener(ArrayList<Byte> arrayList, IAnnouncementListener iAnnouncementListener, registerAnnouncementListenerCallback registerannouncementlistenercallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IBroadcastRadio asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IBroadcastRadio)) {
            return (IBroadcastRadio) iface;
        }
        IBroadcastRadio proxy = new Proxy(binder);
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

    static IBroadcastRadio castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IBroadcastRadio getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IBroadcastRadio getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IBroadcastRadio getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IBroadcastRadio getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IBroadcastRadio {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.broadcastradio@2.0::IBroadcastRadio]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio
        public Properties getProperties() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBroadcastRadio.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                Properties _hidl_out_properties = new Properties();
                _hidl_out_properties.readFromParcel(_hidl_reply);
                return _hidl_out_properties;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio
        public void getAmFmRegionConfig(boolean full, getAmFmRegionConfigCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBroadcastRadio.kInterfaceName);
            _hidl_request.writeBool(full);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                AmFmRegionConfig _hidl_out_config = new AmFmRegionConfig();
                _hidl_out_config.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_config);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio
        public void getDabRegionConfig(getDabRegionConfigCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBroadcastRadio.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                ArrayList<DabTableEntry> _hidl_out_config = DabTableEntry.readVectorFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_config);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio
        public void openSession(ITunerCallback callback, openSessionCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBroadcastRadio.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                ITunerSession _hidl_out_session = ITunerSession.asInterface(_hidl_reply.readStrongBinder());
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_session);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio
        public ArrayList<Byte> getImage(int id) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBroadcastRadio.kInterfaceName);
            _hidl_request.writeInt32(id);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<Byte> _hidl_out_image = _hidl_reply.readInt8Vector();
                return _hidl_out_image;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio
        public void registerAnnouncementListener(ArrayList<Byte> enabled, IAnnouncementListener listener, registerAnnouncementListenerCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBroadcastRadio.kInterfaceName);
            _hidl_request.writeInt8Vector(enabled);
            _hidl_request.writeStrongBinder(listener == null ? null : listener.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                ICloseHandle _hidl_out_closeHandle = ICloseHandle.asInterface(_hidl_reply.readStrongBinder());
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_closeHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IBroadcastRadio {
        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IBroadcastRadio.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IBroadcastRadio.kInterfaceName;
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{68, 1, 124, 66, -26, -12, -40, -53, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, -16, 126, -79, -38, 4, 84, 10, -104, 115, 106, 51, 106, -62, -116, 126, AcquiredInfo.RECALIBRATE, -46, -26, -98, AcquiredInfo.START, -119, -8, -47}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IBroadcastRadio.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(IBroadcastRadio.kInterfaceName);
                    Properties _hidl_out_properties = getProperties();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_properties.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(IBroadcastRadio.kInterfaceName);
                    boolean full = _hidl_request.readBool();
                    getAmFmRegionConfig(full, new getAmFmRegionConfigCallback() { // from class: android.hardware.broadcastradio.V2_0.IBroadcastRadio.Stub.1
                        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.getAmFmRegionConfigCallback
                        public void onValues(int result, AmFmRegionConfig config) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            config.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    _hidl_request.enforceInterface(IBroadcastRadio.kInterfaceName);
                    getDabRegionConfig(new getDabRegionConfigCallback() { // from class: android.hardware.broadcastradio.V2_0.IBroadcastRadio.Stub.2
                        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.getDabRegionConfigCallback
                        public void onValues(int result, ArrayList<DabTableEntry> config) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            DabTableEntry.writeVectorToParcel(_hidl_reply, config);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(IBroadcastRadio.kInterfaceName);
                    ITunerCallback callback = ITunerCallback.asInterface(_hidl_request.readStrongBinder());
                    openSession(callback, new openSessionCallback() { // from class: android.hardware.broadcastradio.V2_0.IBroadcastRadio.Stub.3
                        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.openSessionCallback
                        public void onValues(int result, ITunerSession session) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeStrongBinder(session == null ? null : session.asBinder());
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 5:
                    _hidl_request.enforceInterface(IBroadcastRadio.kInterfaceName);
                    int id = _hidl_request.readInt32();
                    ArrayList<Byte> _hidl_out_image = getImage(id);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt8Vector(_hidl_out_image);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(IBroadcastRadio.kInterfaceName);
                    ArrayList<Byte> enabled = _hidl_request.readInt8Vector();
                    IAnnouncementListener listener = IAnnouncementListener.asInterface(_hidl_request.readStrongBinder());
                    registerAnnouncementListener(enabled, listener, new registerAnnouncementListenerCallback() { // from class: android.hardware.broadcastradio.V2_0.IBroadcastRadio.Stub.4
                        @Override // android.hardware.broadcastradio.V2_0.IBroadcastRadio.registerAnnouncementListenerCallback
                        public void onValues(int result, ICloseHandle closeHandle) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(result);
                            _hidl_reply.writeStrongBinder(closeHandle == null ? null : closeHandle.asBinder());
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
