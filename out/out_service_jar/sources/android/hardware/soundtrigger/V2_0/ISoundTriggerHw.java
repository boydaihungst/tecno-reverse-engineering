package android.hardware.soundtrigger.V2_0;

import android.hardware.audio.common.V2_0.AudioDevice;
import android.hardware.audio.common.V2_0.Uuid;
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
public interface ISoundTriggerHw extends IBase {
    public static final String kInterfaceName = "android.hardware.soundtrigger@2.0::ISoundTriggerHw";

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface getPropertiesCallback {
        void onValues(int i, Properties properties);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface loadPhraseSoundModelCallback {
        void onValues(int i, int i2);
    }

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface loadSoundModelCallback {
        void onValues(int i, int i2);
    }

    @Override // android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    void getProperties(getPropertiesCallback getpropertiescallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    void loadPhraseSoundModel(PhraseSoundModel phraseSoundModel, ISoundTriggerHwCallback iSoundTriggerHwCallback, int i, loadPhraseSoundModelCallback loadphrasesoundmodelcallback) throws RemoteException;

    void loadSoundModel(SoundModel soundModel, ISoundTriggerHwCallback iSoundTriggerHwCallback, int i, loadSoundModelCallback loadsoundmodelcallback) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    int startRecognition(int i, RecognitionConfig recognitionConfig, ISoundTriggerHwCallback iSoundTriggerHwCallback, int i2) throws RemoteException;

    int stopAllRecognitions() throws RemoteException;

    int stopRecognition(int i) throws RemoteException;

    @Override // android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    int unloadSoundModel(int i) throws RemoteException;

    static ISoundTriggerHw asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof ISoundTriggerHw)) {
            return (ISoundTriggerHw) iface;
        }
        ISoundTriggerHw proxy = new Proxy(binder);
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

    static ISoundTriggerHw castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static ISoundTriggerHw getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static ISoundTriggerHw getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR, retry);
    }

    @Deprecated
    static ISoundTriggerHw getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static ISoundTriggerHw getService() throws RemoteException {
        return getService(HealthServiceWrapperHidl.INSTANCE_VENDOR);
    }

    /* loaded from: classes.dex */
    public static final class Properties {
        public String implementor = new String();
        public String description = new String();
        public int version = 0;
        public Uuid uuid = new Uuid();
        public int maxSoundModels = 0;
        public int maxKeyPhrases = 0;
        public int maxUsers = 0;
        public int recognitionModes = 0;
        public boolean captureTransition = false;
        public int maxBufferMs = 0;
        public boolean concurrentCapture = false;
        public boolean triggerInEvent = false;
        public int powerConsumptionMw = 0;

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != Properties.class) {
                return false;
            }
            Properties other = (Properties) otherObject;
            if (HidlSupport.deepEquals(this.implementor, other.implementor) && HidlSupport.deepEquals(this.description, other.description) && this.version == other.version && HidlSupport.deepEquals(this.uuid, other.uuid) && this.maxSoundModels == other.maxSoundModels && this.maxKeyPhrases == other.maxKeyPhrases && this.maxUsers == other.maxUsers && this.recognitionModes == other.recognitionModes && this.captureTransition == other.captureTransition && this.maxBufferMs == other.maxBufferMs && this.concurrentCapture == other.concurrentCapture && this.triggerInEvent == other.triggerInEvent && this.powerConsumptionMw == other.powerConsumptionMw) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.implementor)), Integer.valueOf(HidlSupport.deepHashCode(this.description)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.version))), Integer.valueOf(HidlSupport.deepHashCode(this.uuid)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxSoundModels))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxKeyPhrases))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxUsers))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.recognitionModes))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.captureTransition))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxBufferMs))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.concurrentCapture))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.triggerInEvent))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.powerConsumptionMw))));
        }

        public final String toString() {
            return "{.implementor = " + this.implementor + ", .description = " + this.description + ", .version = " + this.version + ", .uuid = " + this.uuid + ", .maxSoundModels = " + this.maxSoundModels + ", .maxKeyPhrases = " + this.maxKeyPhrases + ", .maxUsers = " + this.maxUsers + ", .recognitionModes = " + this.recognitionModes + ", .captureTransition = " + this.captureTransition + ", .maxBufferMs = " + this.maxBufferMs + ", .concurrentCapture = " + this.concurrentCapture + ", .triggerInEvent = " + this.triggerInEvent + ", .powerConsumptionMw = " + this.powerConsumptionMw + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(88L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<Properties> readVectorFromParcel(HwParcel parcel) {
            ArrayList<Properties> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 88, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                Properties _hidl_vec_element = new Properties();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 88);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            String string = _hidl_blob.getString(_hidl_offset + 0);
            this.implementor = string;
            parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 0 + 0, false);
            String string2 = _hidl_blob.getString(_hidl_offset + 16);
            this.description = string2;
            parcel.readEmbeddedBuffer(string2.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 16 + 0, false);
            this.version = _hidl_blob.getInt32(_hidl_offset + 32);
            this.uuid.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 36);
            this.maxSoundModels = _hidl_blob.getInt32(_hidl_offset + 52);
            this.maxKeyPhrases = _hidl_blob.getInt32(_hidl_offset + 56);
            this.maxUsers = _hidl_blob.getInt32(_hidl_offset + 60);
            this.recognitionModes = _hidl_blob.getInt32(_hidl_offset + 64);
            this.captureTransition = _hidl_blob.getBool(_hidl_offset + 68);
            this.maxBufferMs = _hidl_blob.getInt32(_hidl_offset + 72);
            this.concurrentCapture = _hidl_blob.getBool(_hidl_offset + 76);
            this.triggerInEvent = _hidl_blob.getBool(_hidl_offset + 77);
            this.powerConsumptionMw = _hidl_blob.getInt32(_hidl_offset + 80);
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(88);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<Properties> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 88);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 88);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            _hidl_blob.putString(0 + _hidl_offset, this.implementor);
            _hidl_blob.putString(16 + _hidl_offset, this.description);
            _hidl_blob.putInt32(32 + _hidl_offset, this.version);
            this.uuid.writeEmbeddedToBlob(_hidl_blob, 36 + _hidl_offset);
            _hidl_blob.putInt32(52 + _hidl_offset, this.maxSoundModels);
            _hidl_blob.putInt32(56 + _hidl_offset, this.maxKeyPhrases);
            _hidl_blob.putInt32(60 + _hidl_offset, this.maxUsers);
            _hidl_blob.putInt32(64 + _hidl_offset, this.recognitionModes);
            _hidl_blob.putBool(68 + _hidl_offset, this.captureTransition);
            _hidl_blob.putInt32(72 + _hidl_offset, this.maxBufferMs);
            _hidl_blob.putBool(76 + _hidl_offset, this.concurrentCapture);
            _hidl_blob.putBool(77 + _hidl_offset, this.triggerInEvent);
            _hidl_blob.putInt32(80 + _hidl_offset, this.powerConsumptionMw);
        }
    }

    /* loaded from: classes.dex */
    public static final class SoundModel {
        public int type = 0;
        public Uuid uuid = new Uuid();
        public Uuid vendorUuid = new Uuid();
        public ArrayList<Byte> data = new ArrayList<>();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != SoundModel.class) {
                return false;
            }
            SoundModel other = (SoundModel) otherObject;
            if (this.type == other.type && HidlSupport.deepEquals(this.uuid, other.uuid) && HidlSupport.deepEquals(this.vendorUuid, other.vendorUuid) && HidlSupport.deepEquals(this.data, other.data)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.uuid)), Integer.valueOf(HidlSupport.deepHashCode(this.vendorUuid)), Integer.valueOf(HidlSupport.deepHashCode(this.data)));
        }

        public final String toString() {
            return "{.type = " + SoundModelType.toString(this.type) + ", .uuid = " + this.uuid + ", .vendorUuid = " + this.vendorUuid + ", .data = " + this.data + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(56L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<SoundModel> readVectorFromParcel(HwParcel parcel) {
            ArrayList<SoundModel> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 56, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                SoundModel _hidl_vec_element = new SoundModel();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 56);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.type = _hidl_blob.getInt32(_hidl_offset + 0);
            this.uuid.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 4);
            this.vendorUuid.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 20);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 40 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 1, _hidl_blob.handle(), _hidl_offset + 40 + 0, true);
            this.data.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                byte _hidl_vec_element = childBlob.getInt8(_hidl_index_0 * 1);
                this.data.add(Byte.valueOf(_hidl_vec_element));
            }
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(56);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<SoundModel> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 56);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 56);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            _hidl_blob.putInt32(_hidl_offset + 0, this.type);
            this.uuid.writeEmbeddedToBlob(_hidl_blob, 4 + _hidl_offset);
            this.vendorUuid.writeEmbeddedToBlob(_hidl_blob, 20 + _hidl_offset);
            int _hidl_vec_size = this.data.size();
            _hidl_blob.putInt32(_hidl_offset + 40 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 40 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt8(_hidl_index_0 * 1, this.data.get(_hidl_index_0).byteValue());
            }
            _hidl_blob.putBlob(40 + _hidl_offset + 0, childBlob);
        }
    }

    /* loaded from: classes.dex */
    public static final class Phrase {
        public int id = 0;
        public int recognitionModes = 0;
        public ArrayList<Integer> users = new ArrayList<>();
        public String locale = new String();
        public String text = new String();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != Phrase.class) {
                return false;
            }
            Phrase other = (Phrase) otherObject;
            if (this.id == other.id && this.recognitionModes == other.recognitionModes && HidlSupport.deepEquals(this.users, other.users) && HidlSupport.deepEquals(this.locale, other.locale) && HidlSupport.deepEquals(this.text, other.text)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.id))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.recognitionModes))), Integer.valueOf(HidlSupport.deepHashCode(this.users)), Integer.valueOf(HidlSupport.deepHashCode(this.locale)), Integer.valueOf(HidlSupport.deepHashCode(this.text)));
        }

        public final String toString() {
            return "{.id = " + this.id + ", .recognitionModes = " + this.recognitionModes + ", .users = " + this.users + ", .locale = " + this.locale + ", .text = " + this.text + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(56L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<Phrase> readVectorFromParcel(HwParcel parcel) {
            ArrayList<Phrase> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 56, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                Phrase _hidl_vec_element = new Phrase();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 56);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.id = _hidl_blob.getInt32(_hidl_offset + 0);
            this.recognitionModes = _hidl_blob.getInt32(_hidl_offset + 4);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 8 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 4, _hidl_blob.handle(), _hidl_offset + 8 + 0, true);
            this.users.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                int _hidl_vec_element = childBlob.getInt32(_hidl_index_0 * 4);
                this.users.add(Integer.valueOf(_hidl_vec_element));
            }
            String string = _hidl_blob.getString(_hidl_offset + 24);
            this.locale = string;
            parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 24 + 0, false);
            String string2 = _hidl_blob.getString(_hidl_offset + 40);
            this.text = string2;
            parcel.readEmbeddedBuffer(string2.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 40 + 0, false);
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(56);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<Phrase> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 56);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 56);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            _hidl_blob.putInt32(_hidl_offset + 0, this.id);
            _hidl_blob.putInt32(4 + _hidl_offset, this.recognitionModes);
            int _hidl_vec_size = this.users.size();
            _hidl_blob.putInt32(_hidl_offset + 8 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 8 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 4);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                childBlob.putInt32(_hidl_index_0 * 4, this.users.get(_hidl_index_0).intValue());
            }
            _hidl_blob.putBlob(8 + _hidl_offset + 0, childBlob);
            _hidl_blob.putString(24 + _hidl_offset, this.locale);
            _hidl_blob.putString(40 + _hidl_offset, this.text);
        }
    }

    /* loaded from: classes.dex */
    public static final class PhraseSoundModel {
        public SoundModel common = new SoundModel();
        public ArrayList<Phrase> phrases = new ArrayList<>();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != PhraseSoundModel.class) {
                return false;
            }
            PhraseSoundModel other = (PhraseSoundModel) otherObject;
            if (HidlSupport.deepEquals(this.common, other.common) && HidlSupport.deepEquals(this.phrases, other.phrases)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.common)), Integer.valueOf(HidlSupport.deepHashCode(this.phrases)));
        }

        public final String toString() {
            return "{.common = " + this.common + ", .phrases = " + this.phrases + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(72L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<PhraseSoundModel> readVectorFromParcel(HwParcel parcel) {
            ArrayList<PhraseSoundModel> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 72, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                PhraseSoundModel _hidl_vec_element = new PhraseSoundModel();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 72);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.common.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 0);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 56 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 56, _hidl_blob.handle(), _hidl_offset + 56 + 0, true);
            this.phrases.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                Phrase _hidl_vec_element = new Phrase();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 56);
                this.phrases.add(_hidl_vec_element);
            }
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(72);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PhraseSoundModel> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 72);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 72);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            this.common.writeEmbeddedToBlob(_hidl_blob, _hidl_offset + 0);
            int _hidl_vec_size = this.phrases.size();
            _hidl_blob.putInt32(_hidl_offset + 56 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 56 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 56);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                this.phrases.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 56);
            }
            _hidl_blob.putBlob(56 + _hidl_offset + 0, childBlob);
        }
    }

    /* loaded from: classes.dex */
    public static final class RecognitionConfig {
        public int captureHandle = 0;
        public int captureDevice = 0;
        public boolean captureRequested = false;
        public ArrayList<PhraseRecognitionExtra> phrases = new ArrayList<>();
        public ArrayList<Byte> data = new ArrayList<>();

        public final boolean equals(Object otherObject) {
            if (this == otherObject) {
                return true;
            }
            if (otherObject == null || otherObject.getClass() != RecognitionConfig.class) {
                return false;
            }
            RecognitionConfig other = (RecognitionConfig) otherObject;
            if (this.captureHandle == other.captureHandle && this.captureDevice == other.captureDevice && this.captureRequested == other.captureRequested && HidlSupport.deepEquals(this.phrases, other.phrases) && HidlSupport.deepEquals(this.data, other.data)) {
                return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.captureHandle))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.captureDevice))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.captureRequested))), Integer.valueOf(HidlSupport.deepHashCode(this.phrases)), Integer.valueOf(HidlSupport.deepHashCode(this.data)));
        }

        public final String toString() {
            return "{.captureHandle = " + this.captureHandle + ", .captureDevice = " + AudioDevice.toString(this.captureDevice) + ", .captureRequested = " + this.captureRequested + ", .phrases = " + this.phrases + ", .data = " + this.data + "}";
        }

        public final void readFromParcel(HwParcel parcel) {
            HwBlob blob = parcel.readBuffer(48L);
            readEmbeddedFromParcel(parcel, blob, 0L);
        }

        public static final ArrayList<RecognitionConfig> readVectorFromParcel(HwParcel parcel) {
            ArrayList<RecognitionConfig> _hidl_vec = new ArrayList<>();
            HwBlob _hidl_blob = parcel.readBuffer(16L);
            int _hidl_vec_size = _hidl_blob.getInt32(8L);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 48, _hidl_blob.handle(), 0L, true);
            _hidl_vec.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                RecognitionConfig _hidl_vec_element = new RecognitionConfig();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
                _hidl_vec.add(_hidl_vec_element);
            }
            return _hidl_vec;
        }

        public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
            this.captureHandle = _hidl_blob.getInt32(_hidl_offset + 0);
            this.captureDevice = _hidl_blob.getInt32(_hidl_offset + 4);
            this.captureRequested = _hidl_blob.getBool(_hidl_offset + 8);
            int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 16 + 8);
            HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), _hidl_offset + 16 + 0, true);
            this.phrases.clear();
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                PhraseRecognitionExtra _hidl_vec_element = new PhraseRecognitionExtra();
                _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
                this.phrases.add(_hidl_vec_element);
            }
            int _hidl_vec_size2 = _hidl_blob.getInt32(_hidl_offset + 32 + 8);
            HwBlob childBlob2 = parcel.readEmbeddedBuffer(_hidl_vec_size2 * 1, _hidl_blob.handle(), _hidl_offset + 32 + 0, true);
            this.data.clear();
            for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
                this.data.add(Byte.valueOf(childBlob2.getInt8(_hidl_index_02 * 1)));
            }
        }

        public final void writeToParcel(HwParcel parcel) {
            HwBlob _hidl_blob = new HwBlob(48);
            writeEmbeddedToBlob(_hidl_blob, 0L);
            parcel.writeBuffer(_hidl_blob);
        }

        public static final void writeVectorToParcel(HwParcel parcel, ArrayList<RecognitionConfig> _hidl_vec) {
            HwBlob _hidl_blob = new HwBlob(16);
            int _hidl_vec_size = _hidl_vec.size();
            _hidl_blob.putInt32(8L, _hidl_vec_size);
            _hidl_blob.putBool(12L, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 48);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 48);
            }
            _hidl_blob.putBlob(0L, childBlob);
            parcel.writeBuffer(_hidl_blob);
        }

        public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
            _hidl_blob.putInt32(_hidl_offset + 0, this.captureHandle);
            _hidl_blob.putInt32(_hidl_offset + 4, this.captureDevice);
            _hidl_blob.putBool(_hidl_offset + 8, this.captureRequested);
            int _hidl_vec_size = this.phrases.size();
            _hidl_blob.putInt32(_hidl_offset + 16 + 8, _hidl_vec_size);
            _hidl_blob.putBool(_hidl_offset + 16 + 12, false);
            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                this.phrases.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 32);
            }
            _hidl_blob.putBlob(_hidl_offset + 16 + 0, childBlob);
            int _hidl_vec_size2 = this.data.size();
            _hidl_blob.putInt32(_hidl_offset + 32 + 8, _hidl_vec_size2);
            _hidl_blob.putBool(_hidl_offset + 32 + 12, false);
            HwBlob childBlob2 = new HwBlob(_hidl_vec_size2 * 1);
            for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
                childBlob2.putInt8(_hidl_index_02 * 1, this.data.get(_hidl_index_02).byteValue());
            }
            _hidl_blob.putBlob(_hidl_offset + 32 + 0, childBlob2);
        }
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements ISoundTriggerHw {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.soundtrigger@2.0::ISoundTriggerHw]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public void getProperties(getPropertiesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                Properties _hidl_out_properties = new Properties();
                _hidl_out_properties.readFromParcel(_hidl_reply);
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_properties);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public void loadSoundModel(SoundModel soundModel, ISoundTriggerHwCallback callback, int cookie, loadSoundModelCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            soundModel.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                int _hidl_out_modelHandle = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_modelHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public void loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHwCallback callback, int cookie, loadPhraseSoundModelCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            soundModel.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                int _hidl_out_modelHandle = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_retval, _hidl_out_modelHandle);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int unloadSoundModel(int modelHandle) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int startRecognition(int modelHandle, RecognitionConfig config, ISoundTriggerHwCallback callback, int cookie) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            config.writeToParcel(_hidl_request);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            _hidl_request.writeInt32(cookie);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int stopRecognition(int modelHandle) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            _hidl_request.writeInt32(modelHandle);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw
        public int stopAllRecognitions() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(ISoundTriggerHw.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_retval = _hidl_reply.readInt32();
                return _hidl_out_retval;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
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

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements ISoundTriggerHw {
        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(ISoundTriggerHw.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return ISoundTriggerHw.kInterfaceName;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{91, -17, -64, AcquiredInfo.DARK_GLASSES_DETECTED, -53, -23, 73, 83, 102, 30, 44, -37, -107, -29, -49, 100, -11, -27, 101, -62, -108, 3, -31, -62, -38, -20, -62, -66, 68, -32, -91, 92}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, UsbDescriptor.DESCRIPTORTYPE_PHYSICAL, -17, 5, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -13, -51, 105, 87, AcquiredInfo.ROLL_TOO_EXTREME, -109, UsbDescriptor.DESCRIPTORTYPE_CLASSSPECIFIC_INTERFACE, -72, 59, AcquiredInfo.FIRST_FRAME_RECEIVED, -54, 76}));
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw, android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (ISoundTriggerHw.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    getProperties(new getPropertiesCallback() { // from class: android.hardware.soundtrigger.V2_0.ISoundTriggerHw.Stub.1
                        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.getPropertiesCallback
                        public void onValues(int retval, Properties properties) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            properties.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 2:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    SoundModel soundModel = new SoundModel();
                    soundModel.readFromParcel(_hidl_request);
                    ISoundTriggerHwCallback callback = ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie = _hidl_request.readInt32();
                    loadSoundModel(soundModel, callback, cookie, new loadSoundModelCallback() { // from class: android.hardware.soundtrigger.V2_0.ISoundTriggerHw.Stub.2
                        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.loadSoundModelCallback
                        public void onValues(int retval, int modelHandle) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt32(modelHandle);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    PhraseSoundModel soundModel2 = new PhraseSoundModel();
                    soundModel2.readFromParcel(_hidl_request);
                    ISoundTriggerHwCallback callback2 = ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie2 = _hidl_request.readInt32();
                    loadPhraseSoundModel(soundModel2, callback2, cookie2, new loadPhraseSoundModelCallback() { // from class: android.hardware.soundtrigger.V2_0.ISoundTriggerHw.Stub.3
                        @Override // android.hardware.soundtrigger.V2_0.ISoundTriggerHw.loadPhraseSoundModelCallback
                        public void onValues(int retval, int modelHandle) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt32(retval);
                            _hidl_reply.writeInt32(modelHandle);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    int modelHandle = _hidl_request.readInt32();
                    int _hidl_out_retval = unloadSoundModel(modelHandle);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    int modelHandle2 = _hidl_request.readInt32();
                    RecognitionConfig config = new RecognitionConfig();
                    config.readFromParcel(_hidl_request);
                    ISoundTriggerHwCallback callback3 = ISoundTriggerHwCallback.asInterface(_hidl_request.readStrongBinder());
                    int cookie3 = _hidl_request.readInt32();
                    int _hidl_out_retval2 = startRecognition(modelHandle2, config, callback3, cookie3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval2);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    int modelHandle3 = _hidl_request.readInt32();
                    int _hidl_out_retval3 = stopRecognition(modelHandle3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval3);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(ISoundTriggerHw.kInterfaceName);
                    int _hidl_out_retval4 = stopAllRecognitions();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_retval4);
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
