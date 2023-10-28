package android.hardware.soundtrigger.V2_0;

import android.hardware.audio.common.V2_0.AudioConfig;
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
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.health.HealthServiceWrapperHidl;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
/* loaded from: classes.dex */
public interface ISoundTriggerHwCallback extends IBase {
    public static final String kInterfaceName = "android.hardware.soundtrigger@2.0::ISoundTriggerHwCallback";

    @Override // android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    void phraseRecognitionCallback(PhraseRecognitionEvent phraseRecognitionEvent, int i) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    void recognitionCallback(RecognitionEvent recognitionEvent, int i) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    void soundModelCallback(ModelEvent modelEvent, int i) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static ISoundTriggerHwCallback asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISoundTriggerHwCallback)) {
            return (ISoundTriggerHwCallback) iface;
        }
        ISoundTriggerHwCallback proxy = new Proxy(binder);
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

    static ISoundTriggerHwCallback castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ISoundTriggerHwCallback getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISoundTriggerHwCallback getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static ISoundTriggerHwCallback getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static ISoundTriggerHwCallback getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class RecognitionStatus {
        public static final int ABORT = 1;
        public static final int FAILURE = 2;
        public static final int SUCCESS = 0;

        public static final String toString(int o) {
            if (o == 0) {
                return "SUCCESS";
            }
            if (o == 1) {
                return "ABORT";
            }
            if (o == 2) {
                return "FAILURE";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            int flipped = 0;
            list.add("SUCCESS");
            if ((o & 1) == 1) {
                list.add("ABORT");
                flipped = 0 | 1;
            }
            if ((o & 2) == 2) {
                list.add("FAILURE");
                flipped |= 2;
            }
            if (o != flipped) {
                list.add("0x" + Integer.toHexString((~flipped) & o));
            }
            return String.join(" | ", list);
        }
    }

    /* loaded from: classes.dex */
    public static final class SoundModelStatus {
        public static final int UPDATED = 0;

        public static final String toString(int o) {
            if (o == 0) {
                return "UPDATED";
            }
            return "0x" + Integer.toHexString(o);
        }

        public static final String dumpBitfield(int o) {
            ArrayList<String> list = new ArrayList<>();
            list.add("UPDATED");
            if (o != 0) {
                list.add("0x" + Integer.toHexString((~0) & o));
            }
            return String.join(" | ", list);
        }
    }

    /* loaded from: classes.dex */
    public static final class RecognitionEvent {
        public int status = 0;
        public int type = 0;
        public int model = 0;
        public boolean captureAvailable = false;
        public int captureSession = 0;
        public int captureDelayMs = 0;
        public int capturePreambleMs = 0;
        public boolean triggerInData = false;
        public AudioConfig audioConfig = new AudioConfig();
        public ArrayList<Byte> data = new ArrayList<>();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != RecognitionEvent.class) {
                return false;
            }
            RecognitionEvent other = (RecognitionEvent) otherObject;
            if (this.status == other.status && this.type == other.type && this.model == other.model && this.captureAvailable == other.captureAvailable && this.captureSession == other.captureSession && this.captureDelayMs == other.captureDelayMs && this.capturePreambleMs == other.capturePreambleMs && this.triggerInData == other.triggerInData && HidlSupport.deepEquals(this.audioConfig, other.audioConfig) && HidlSupport.deepEquals(this.data, other.data)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.status))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.model))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.captureAvailable))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.captureSession))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.captureDelayMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.capturePreambleMs))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.triggerInData))), Integer.valueOf(HidlSupport.deepHashCode(this.audioConfig)), Integer.valueOf(HidlSupport.deepHashCode(this.data)));
        }

        public final String toString() {
            return "{.status = " + RecognitionStatus.toString(this.status) + ", .type = " + SoundModelType.toString(this.type) + ", .model = " + this.model + ", .captureAvailable = " + this.captureAvailable + ", .captureSession = " + this.captureSession + ", .captureDelayMs = " + this.captureDelayMs + ", .capturePreambleMs = " + this.capturePreambleMs + ", .triggerInData = " + this.triggerInData + ", .audioConfig = " + this.audioConfig + ", .data = " + this.data + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(120L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<RecognitionEvent> readVectorFromParcel(HwParcel parcel) {
            ArrayList<RecognitionEvent> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 120, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                RecognitionEvent _hidl_vec_element = new RecognitionEvent();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 120);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.status = _hidl_blob.getInt32(_hidl_offset + 0);
            this.type = _hidl_blob.getInt32(_hidl_offset + 4);
            this.model = _hidl_blob.getInt32(_hidl_offset + 8);
            this.captureAvailable = _hidl_blob.getBool(_hidl_offset + 12);
            this.captureSession = _hidl_blob.getInt32(_hidl_offset + 16);
            this.captureDelayMs = _hidl_blob.getInt32(_hidl_offset + 20);
            this.capturePreambleMs = _hidl_blob.getInt32(_hidl_offset + 24);
            this.triggerInData = _hidl_blob.getBool(_hidl_offset + 28);
            this.audioConfig.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 32);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 104 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 1, _hidl_blob.handle(), 0 + _hidl_offset + 104, true);
            this.data.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                byte _hidl_vec_element = childBlob.getInt8(_hidl_index_0 * 1);
                this.data.add(Byte.valueOf(_hidl_vec_element));
            }
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(120);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<RecognitionEvent> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 120);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 120);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            _hidl_blob.putInt32(_hidl_offset + 0, this.status);
            _hidl_blob.putInt32(4 + _hidl_offset, this.type);
            _hidl_blob.putInt32(_hidl_offset + 8, this.model);
            _hidl_blob.putBool(_hidl_offset + 12, this.captureAvailable);
            _hidl_blob.putInt32(16 + _hidl_offset, this.captureSession);
            _hidl_blob.putInt32(20 + _hidl_offset, this.captureDelayMs);
            _hidl_blob.putInt32(24 + _hidl_offset, this.capturePreambleMs);
            _hidl_blob.putBool(28 + _hidl_offset, this.triggerInData);
            this.audioConfig.writeEmbeddedToBlob(_hidl_blob, 32 + _hidl_offset);
            int _hidl_vec_size = this.data.size();
            _hidl_blob.putInt32(_hidl_offset + 104 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 104 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt8(_hidl_index_0 * 1, this.data.get(_hidl_index_0).byteValue());
            }
            _hidl_blob.putBlob(104 + _hidl_offset + 0, childBlob);
        }
    }

    /* loaded from: classes.dex */
    public static final class PhraseRecognitionEvent {
        public RecognitionEvent common = new RecognitionEvent();
        public ArrayList<PhraseRecognitionExtra> phraseExtras = new ArrayList<>();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != PhraseRecognitionEvent.class) {
                return false;
            }
            PhraseRecognitionEvent other = (PhraseRecognitionEvent) otherObject;
            if (HidlSupport.deepEquals(this.common, other.common) && HidlSupport.deepEquals(this.phraseExtras, other.phraseExtras)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.common)), Integer.valueOf(HidlSupport.deepHashCode(this.phraseExtras)));
        }

        public final String toString() {
            return "{.common = " + this.common + ", .phraseExtras = " + this.phraseExtras + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(136L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<PhraseRecognitionEvent> readVectorFromParcel(HwParcel parcel) {
            ArrayList<PhraseRecognitionEvent> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                PhraseRecognitionEvent _hidl_vec_element = new PhraseRecognitionEvent();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.common.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 0);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 120 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), _hidl_offset + 120 + 0, true);
            this.phraseExtras.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                PhraseRecognitionExtra _hidl_vec_element = new PhraseRecognitionExtra();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
                this.phraseExtras.add(_hidl_vec_element);
            }
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob((int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PhraseRecognitionEvent> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            this.common.writeEmbeddedToBlob(_hidl_blob, _hidl_offset + 0);
            int _hidl_vec_size = this.phraseExtras.size();
            _hidl_blob.putInt32(_hidl_offset + 120 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 120 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                this.phraseExtras.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 32);
            }
            _hidl_blob.putBlob(120 + _hidl_offset + 0, childBlob);
        }
    }

    /* loaded from: classes.dex */
    public static final class ModelEvent {
        public int status = 0;
        public int model = 0;
        public ArrayList<Byte> data = new ArrayList<>();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != ModelEvent.class) {
                return false;
            }
            ModelEvent other = (ModelEvent) otherObject;
            if (this.status == other.status && this.model == other.model && HidlSupport.deepEquals(this.data, other.data)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.status))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.model))), Integer.valueOf(HidlSupport.deepHashCode(this.data)));
        }

        public final String toString() {
            return "{.status = " + SoundModelStatus.toString(this.status) + ", .model = " + this.model + ", .data = " + this.data + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(24L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<ModelEvent> readVectorFromParcel(HwParcel parcel) {
            ArrayList<ModelEvent> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 24, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                ModelEvent _hidl_vec_element = new ModelEvent();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 24);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.status = _hidl_blob.getInt32(_hidl_offset + 0);
            this.model = _hidl_blob.getInt32(_hidl_offset + 4);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 8 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 1, _hidl_blob.handle(), _hidl_offset + 8 + 0, true);
            this.data.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                byte _hidl_vec_element = childBlob.getInt8(_hidl_index_0 * 1);
                this.data.add(Byte.valueOf(_hidl_vec_element));
            }
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(24);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ModelEvent> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 24);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 24);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            _hidl_blob.putInt32(_hidl_offset + 0, this.status);
            _hidl_blob.putInt32(4 + _hidl_offset, this.model);
            int _hidl_vec_size = this.data.size();
            _hidl_blob.putInt32(_hidl_offset + 8 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 8 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt8(_hidl_index_0 * 1, this.data.get(_hidl_index_0).byteValue());
            }
            _hidl_blob.putBlob(8 + _hidl_offset + 0, childBlob);
        }
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements ISoundTriggerHwCallback {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.soundtrigger@2.0::ISoundTriggerHwCallback]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback
        public void recognitionCallback(RecognitionEvent event, int cookie) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHwCallback.kInterfaceName);
            event.writeToParcel(_hidl_request);
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback
        public void phraseRecognitionCallback(PhraseRecognitionEvent event, int cookie) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHwCallback.kInterfaceName);
            event.writeToParcel(_hidl_request);
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback
        public void soundModelCallback(ModelEvent event, int cookie) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHwCallback.kInterfaceName);
            event.writeToParcel(_hidl_request);
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements ISoundTriggerHwCallback {
        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISoundTriggerHwCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return ISoundTriggerHwCallback.kInterfaceName;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{AcquiredInfo.MOUTH_COVERING_DETECTED, 110, 43, -46, -119, -14, UsbDescriptor.DESCRIPTORTYPE_HUB, 49, -59, 38, -78, AcquiredInfo.DARK_GLASSES_DETECTED, AcquiredInfo.SENSOR_DIRTY, -111, 15, 29, 76, 67, 107, 122, -53, -107, 86, -28, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, 61, -28, -50, -114, 108, -62, -28}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ISoundTriggerHwCallback.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(ISoundTriggerHwCallback.kInterfaceName);
                    RecognitionEvent event = new RecognitionEvent();
                    event.readFromParcel(_hidl_request);
                    int cookie = _hidl_request.readInt32();
                    recognitionCallback(event, cookie);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(ISoundTriggerHwCallback.kInterfaceName);
                    PhraseRecognitionEvent event2 = new PhraseRecognitionEvent();
                    event2.readFromParcel(_hidl_request);
                    int cookie2 = _hidl_request.readInt32();
                    phraseRecognitionCallback(event2, cookie2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(ISoundTriggerHwCallback.kInterfaceName);
                    ModelEvent event3 = new ModelEvent();
                    event3.readFromParcel(_hidl_request);
                    int cookie3 = _hidl_request.readInt32();
                    soundModelCallback(event3, cookie3);
                    _hidl_reply.writeStatus(0);
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
