package android.hardware.boot.V1_2;

import android.hardware.biometrics.face.AcquiredInfo;
import android.hardware.boot.V1_0.CommandResult;
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
public interface IBootControl extends android.hardware.boot.V1_1.IBootControl {
    public static final String kInterfaceName = "android.hardware.boot@1.2::IBootControl";

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    int getActiveBootSlot() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IBootControl asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IBootControl)) {
            return (IBootControl) iface;
        }
        IBootControl proxy = new Proxy(binder);
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

    static IBootControl castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IBootControl getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IBootControl getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static IBootControl getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IBootControl getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IBootControl {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.boot@1.2::IBootControl]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public int getNumberSlots() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_numSlots = _hidl_reply.readInt32();
                return _hidl_out_numSlots;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public int getCurrentSlot() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_slot = _hidl_reply.readInt32();
                return _hidl_out_slot;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public CommandResult markBootSuccessful() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                CommandResult _hidl_out_error = new CommandResult();
                _hidl_out_error.readFromParcel(_hidl_reply);
                return _hidl_out_error;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public CommandResult setActiveBootSlot(int slot) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            _hidl_request.writeInt32(slot);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                CommandResult _hidl_out_error = new CommandResult();
                _hidl_out_error.readFromParcel(_hidl_reply);
                return _hidl_out_error;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public CommandResult setSlotAsUnbootable(int slot) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            _hidl_request.writeInt32(slot);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                CommandResult _hidl_out_error = new CommandResult();
                _hidl_out_error.readFromParcel(_hidl_reply);
                return _hidl_out_error;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public int isSlotBootable(int slot) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            _hidl_request.writeInt32(slot);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_bootable = _hidl_reply.readInt32();
                return _hidl_out_bootable;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public int isSlotMarkedSuccessful(int slot) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            _hidl_request.writeInt32(slot);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_successful = _hidl_reply.readInt32();
                return _hidl_out_successful;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_0.IBootControl
        public String getSuffix(int slot) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
            _hidl_request.writeInt32(slot);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_slotSuffix = _hidl_reply.readString();
                return _hidl_out_slotSuffix;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_1.IBootControl
        public boolean setSnapshotMergeStatus(int status) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_1.IBootControl.kInterfaceName);
            _hidl_request.writeInt32(status);
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

        @Override // android.hardware.boot.V1_1.IBootControl
        public int getSnapshotMergeStatus() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(android.hardware.boot.V1_1.IBootControl.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_2.IBootControl
        public int getActiveBootSlot() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBootControl.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_slot = _hidl_reply.readInt32();
                return _hidl_out_slot;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IBootControl {
        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IBootControl.kInterfaceName, android.hardware.boot.V1_1.IBootControl.kInterfaceName, android.hardware.boot.V1_0.IBootControl.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IBootControl.kInterfaceName;
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{103, 99, -35, UsbDescriptor.DESCRIPTORTYPE_REPORT, 115, -79, -76, Byte.MAX_VALUE, 58, -58, -118, -7, -74, 104, 112, 40, 126, -70, 51, -5, 91, 77, 102, -24, -2, 29, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, -82, AcquiredInfo.FIRST_FRAME_RECEIVED, -50, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -53}, new byte[]{7, -48, -94, 82, -78, -40, -6, 53, -120, 121, 8, -87, -106, -70, 57, 92, -13, -110, -106, UsbASFormat.EXT_FORMAT_TYPE_III, -107, -4, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, -81, -85, 121, 31, 70, -32, -62, UsbDescriptor.DESCRIPTORTYPE_SUPERSPEED_HUB, 82}, new byte[]{113, -110, -41, 86, -82, -70, 0, -85, -93, 47, 69, 4, -104, 29, -8, AcquiredInfo.VENDOR, 47, -4, -88, 62, UsbDescriptor.DESCRIPTORTYPE_HID, 12, 72, 56, -38, -65, UsbDescriptor.DESCRIPTORTYPE_HUB, 94, 83, -23, 53, -112}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.boot.V1_2.IBootControl, android.hardware.boot.V1_1.IBootControl, android.hardware.boot.V1_0.IBootControl, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IBootControl.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int _hidl_out_numSlots = getNumberSlots();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_numSlots);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int _hidl_out_slot = getCurrentSlot();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_slot);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    CommandResult _hidl_out_error = markBootSuccessful();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_error.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int slot = _hidl_request.readInt32();
                    CommandResult _hidl_out_error2 = setActiveBootSlot(slot);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_error2.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int slot2 = _hidl_request.readInt32();
                    CommandResult _hidl_out_error3 = setSlotAsUnbootable(slot2);
                    _hidl_reply.writeStatus(0);
                    _hidl_out_error3.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int slot3 = _hidl_request.readInt32();
                    int _hidl_out_bootable = isSlotBootable(slot3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_bootable);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int slot4 = _hidl_request.readInt32();
                    int _hidl_out_successful = isSlotMarkedSuccessful(slot4);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_successful);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_0.IBootControl.kInterfaceName);
                    int slot5 = _hidl_request.readInt32();
                    String _hidl_out_slotSuffix = getSuffix(slot5);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_slotSuffix);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_1.IBootControl.kInterfaceName);
                    int status = _hidl_request.readInt32();
                    boolean _hidl_out_success = setSnapshotMergeStatus(status);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeBool(_hidl_out_success);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(android.hardware.boot.V1_1.IBootControl.kInterfaceName);
                    int _hidl_out_status = getSnapshotMergeStatus();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(IBootControl.kInterfaceName);
                    int _hidl_out_slot2 = getActiveBootSlot();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_slot2);
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
