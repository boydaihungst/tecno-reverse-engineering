package com.android.server.sensorprivacy;

import android.os.Environment;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PersistedState {
    private static final int CURRENT_PERSISTENCE_VERSION = 2;
    private static final int CURRENT_VERSION = 2;
    private static final String LOG_TAG = PersistedState.class.getSimpleName();
    private static final String XML_ATTRIBUTE_LAST_CHANGE = "last-change";
    private static final String XML_ATTRIBUTE_PERSISTENCE_VERSION = "persistence-version";
    private static final String XML_ATTRIBUTE_SENSOR = "sensor";
    private static final String XML_ATTRIBUTE_STATE_TYPE = "state-type";
    private static final String XML_ATTRIBUTE_TOGGLE_TYPE = "toggle-type";
    private static final String XML_ATTRIBUTE_USER_ID = "user-id";
    private static final String XML_ATTRIBUTE_VERSION = "version";
    private static final String XML_TAG_SENSOR_PRIVACY = "sensor-privacy";
    private static final String XML_TAG_SENSOR_STATE = "sensor-state";
    private final AtomicFile mAtomicFile;
    private ArrayMap<TypeUserSensor, SensorState> mStates = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PersistedState fromFile(String fileName) {
        return new PersistedState(fileName);
    }

    private PersistedState(String fileName) {
        this.mAtomicFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), fileName));
        readState();
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:37:0x00c1 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:49:0x00dd */
    private void readState() {
        AtomicFile file = this.mAtomicFile;
        if (!file.exists()) {
            AtomicFile fileToMigrateFrom = new AtomicFile(new File(Environment.getDataSystemDirectory(), "sensor_privacy.xml"));
            if (fileToMigrateFrom.exists()) {
                try {
                    FileInputStream inputStream = fileToMigrateFrom.openRead();
                    try {
                        XmlUtils.beginDocument(Xml.resolvePullParser(inputStream), XML_TAG_SENSOR_PRIVACY);
                        file = fileToMigrateFrom;
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Throwable th) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Caught an exception reading the state from storage: ", e);
                    fileToMigrateFrom.delete();
                } catch (XmlPullParserException e2) {
                }
            }
        }
        Object nonupgradedState = null;
        if (file.exists()) {
            try {
                FileInputStream inputStream2 = file.openRead();
                TypedXmlPullParser parser = Xml.resolvePullParser(inputStream2);
                XmlUtils.beginDocument(parser, XML_TAG_SENSOR_PRIVACY);
                int persistenceVersion = parser.getAttributeInt((String) null, XML_ATTRIBUTE_PERSISTENCE_VERSION, 0);
                if (persistenceVersion == 0) {
                    PVersion0 version0 = new PVersion0(0);
                    nonupgradedState = version0;
                    readPVersion0(parser, version0);
                } else if (persistenceVersion == 1) {
                    int version = parser.getAttributeInt((String) null, XML_ATTRIBUTE_VERSION, 1);
                    PVersion1 version1 = new PVersion1(version);
                    nonupgradedState = version1;
                    readPVersion1(parser, version1);
                } else if (persistenceVersion == 2) {
                    int version2 = parser.getAttributeInt((String) null, XML_ATTRIBUTE_VERSION, 2);
                    PVersion2 version22 = new PVersion2(version2);
                    nonupgradedState = version22;
                    readPVersion2(parser, version22);
                } else {
                    Log.e(LOG_TAG, "Unknown persistence version: " + persistenceVersion + ". Deleting.", new RuntimeException());
                    file.delete();
                    nonupgradedState = null;
                }
                if (inputStream2 != null) {
                    inputStream2.close();
                }
            } catch (IOException | RuntimeException | XmlPullParserException e3) {
                Log.e(LOG_TAG, "Caught an exception reading the state from storage: ", e3);
                file.delete();
                nonupgradedState = null;
            }
        }
        if (nonupgradedState == null) {
            nonupgradedState = new PVersion2(2);
        }
        boolean z = nonupgradedState instanceof PVersion0;
        Object nonupgradedState2 = nonupgradedState;
        if (z) {
            nonupgradedState2 = PVersion1.fromPVersion0((PVersion0) nonupgradedState);
        }
        boolean z2 = nonupgradedState2 instanceof PVersion1;
        Object nonupgradedState3 = nonupgradedState2;
        if (z2) {
            nonupgradedState3 = PVersion2.fromPVersion1((PVersion1) nonupgradedState2);
        }
        if (nonupgradedState3 instanceof PVersion2) {
            PVersion2 upgradedState = (PVersion2) nonupgradedState3;
            this.mStates = upgradedState.mStates;
            return;
        }
        Log.e(LOG_TAG, "State not successfully upgraded.");
        this.mStates = new ArrayMap<>();
    }

    private static void readPVersion0(TypedXmlPullParser parser, PVersion0 version0) throws XmlPullParserException, IOException {
        XmlUtils.nextElement(parser);
        while (parser.getEventType() != 1) {
            if ("individual-sensor-privacy".equals(parser.getName())) {
                int sensor = XmlUtils.readIntAttribute(parser, XML_ATTRIBUTE_SENSOR);
                boolean indEnabled = XmlUtils.readBooleanAttribute(parser, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
                version0.addState(sensor, indEnabled);
                XmlUtils.skipCurrentTag(parser);
            } else {
                XmlUtils.nextElement(parser);
            }
        }
    }

    private static void readPVersion1(TypedXmlPullParser parser, PVersion1 version1) throws XmlPullParserException, IOException {
        while (parser.getEventType() != 1) {
            XmlUtils.nextElement(parser);
            if ("user".equals(parser.getName())) {
                int currentUserId = parser.getAttributeInt((String) null, "id");
                int depth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, depth)) {
                    if ("individual-sensor-privacy".equals(parser.getName())) {
                        int sensor = parser.getAttributeInt((String) null, XML_ATTRIBUTE_SENSOR);
                        boolean isEnabled = parser.getAttributeBoolean((String) null, ServiceConfigAccessor.PROVIDER_MODE_ENABLED);
                        version1.addState(currentUserId, sensor, isEnabled);
                    }
                }
            }
        }
    }

    private static void readPVersion2(TypedXmlPullParser parser, PVersion2 version2) throws XmlPullParserException, IOException {
        while (parser.getEventType() != 1) {
            XmlUtils.nextElement(parser);
            if (XML_TAG_SENSOR_STATE.equals(parser.getName())) {
                int toggleType = parser.getAttributeInt((String) null, XML_ATTRIBUTE_TOGGLE_TYPE);
                int userId = parser.getAttributeInt((String) null, XML_ATTRIBUTE_USER_ID);
                int sensor = parser.getAttributeInt((String) null, XML_ATTRIBUTE_SENSOR);
                int state = parser.getAttributeInt((String) null, XML_ATTRIBUTE_STATE_TYPE);
                long lastChange = parser.getAttributeLong((String) null, XML_ATTRIBUTE_LAST_CHANGE);
                version2.addState(toggleType, userId, sensor, state, lastChange);
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    public SensorState getState(int toggleType, int userId, int sensor) {
        return this.mStates.get(new TypeUserSensor(toggleType, userId, sensor));
    }

    public SensorState setState(int toggleType, int userId, int sensor, SensorState sensorState) {
        return this.mStates.put(new TypeUserSensor(toggleType, userId, sensor), sensorState);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class TypeUserSensor {
        int mSensor;
        int mType;
        int mUserId;

        TypeUserSensor(int type, int userId, int sensor) {
            this.mType = type;
            this.mUserId = userId;
            this.mSensor = sensor;
        }

        TypeUserSensor(TypeUserSensor typeUserSensor) {
            this(typeUserSensor.mType, typeUserSensor.mUserId, typeUserSensor.mSensor);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof TypeUserSensor) {
                TypeUserSensor that = (TypeUserSensor) o;
                return this.mType == that.mType && this.mUserId == that.mUserId && this.mSensor == that.mSensor;
            }
            return false;
        }

        public int hashCode() {
            return (((this.mType * 31) + this.mUserId) * 31) + this.mSensor;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void schedulePersist() {
        int numStates = this.mStates.size();
        ArrayMap<TypeUserSensor, SensorState> statesCopy = new ArrayMap<>();
        for (int i = 0; i < numStates; i++) {
            statesCopy.put(new TypeUserSensor(this.mStates.keyAt(i)), new SensorState(this.mStates.valueAt(i)));
        }
        IoThread.getHandler().sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.sensorprivacy.PersistedState$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((PersistedState) obj).persist((ArrayMap) obj2);
            }
        }, this, statesCopy));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persist(ArrayMap<TypeUserSensor, SensorState> states) {
        FileOutputStream outputStream = null;
        try {
            outputStream = this.mAtomicFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(outputStream);
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, XML_TAG_SENSOR_PRIVACY);
            serializer.attributeInt((String) null, XML_ATTRIBUTE_PERSISTENCE_VERSION, 2);
            serializer.attributeInt((String) null, XML_ATTRIBUTE_VERSION, 2);
            for (int i = 0; i < states.size(); i++) {
                TypeUserSensor userSensor = states.keyAt(i);
                SensorState sensorState = states.valueAt(i);
                if (userSensor.mType == 1) {
                    serializer.startTag((String) null, XML_TAG_SENSOR_STATE);
                    serializer.attributeInt((String) null, XML_ATTRIBUTE_TOGGLE_TYPE, userSensor.mType);
                    serializer.attributeInt((String) null, XML_ATTRIBUTE_USER_ID, userSensor.mUserId);
                    serializer.attributeInt((String) null, XML_ATTRIBUTE_SENSOR, userSensor.mSensor);
                    serializer.attributeInt((String) null, XML_ATTRIBUTE_STATE_TYPE, sensorState.getState());
                    serializer.attributeLong((String) null, XML_ATTRIBUTE_LAST_CHANGE, sensorState.getLastChange());
                    serializer.endTag((String) null, XML_TAG_SENSOR_STATE);
                }
            }
            serializer.endTag((String) null, XML_TAG_SENSOR_PRIVACY);
            serializer.endDocument();
            this.mAtomicFile.finishWrite(outputStream);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Caught an exception persisting the sensor privacy state: ", e);
            this.mAtomicFile.failWrite(outputStream);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(DualDumpOutputStream dumpStream) {
        SparseArray<SparseArray<Pair<Integer, SensorState>>> statesMatrix = new SparseArray<>();
        int numStates = this.mStates.size();
        for (int i = 0; i < numStates; i++) {
            int toggleType = this.mStates.keyAt(i).mType;
            int userId = this.mStates.keyAt(i).mUserId;
            int sensor = this.mStates.keyAt(i).mSensor;
            SparseArray<Pair<Integer, SensorState>> userStates = statesMatrix.get(userId);
            if (userStates == null) {
                userStates = new SparseArray<>();
                statesMatrix.put(userId, userStates);
            }
            userStates.put(sensor, new Pair<>(Integer.valueOf(toggleType), this.mStates.valueAt(i)));
        }
        dumpStream.write("storage_implementation", 1138166333444L, SensorPrivacyStateControllerImpl.class.getName());
        int numUsers = statesMatrix.size();
        int i2 = 0;
        while (i2 < numUsers) {
            int userId2 = statesMatrix.keyAt(i2);
            long userToken = dumpStream.start(DatabaseHelper.SoundModelContract.KEY_USERS, 2246267895811L);
            dumpStream.write("user_id", (long) CompanionMessage.MESSAGE_ID, userId2);
            SparseArray<Pair<Integer, SensorState>> userStates2 = statesMatrix.valueAt(i2);
            int numSensors = userStates2.size();
            int j = 0;
            while (j < numSensors) {
                int sensor2 = userStates2.keyAt(j);
                int toggleType2 = ((Integer) userStates2.valueAt(j).first).intValue();
                SensorState sensorState = (SensorState) userStates2.valueAt(j).second;
                long sensorToken = dumpStream.start("sensors", 2246267895812L);
                int numSensors2 = numSensors;
                dumpStream.write(XML_ATTRIBUTE_SENSOR, (long) CompanionMessage.MESSAGE_ID, sensor2);
                long toggleToken = dumpStream.start("toggles", 2246267895810L);
                dumpStream.write("toggle_type", 1159641169924L, toggleType2);
                dumpStream.write("state_type", 1159641169925L, sensorState.getState());
                int j2 = numStates;
                dumpStream.write("last_change", 1112396529667L, sensorState.getLastChange());
                dumpStream.end(toggleToken);
                dumpStream.end(sensorToken);
                j++;
                numSensors = numSensors2;
                statesMatrix = statesMatrix;
                numStates = j2;
                numUsers = numUsers;
                i2 = i2;
            }
            dumpStream.end(userToken);
            i2++;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachKnownState(QuadConsumer<Integer, Integer, Integer, SensorState> consumer) {
        int numStates = this.mStates.size();
        for (int i = 0; i < numStates; i++) {
            TypeUserSensor tus = this.mStates.keyAt(i);
            SensorState sensorState = this.mStates.valueAt(i);
            consumer.accept(Integer.valueOf(tus.mType), Integer.valueOf(tus.mUserId), Integer.valueOf(tus.mSensor), sensorState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PVersion0 {
        private SparseArray<SensorState> mIndividualEnabled;

        private PVersion0(int version) {
            this.mIndividualEnabled = new SparseArray<>();
            if (version != 0) {
                throw new RuntimeException("Only version 0 supported");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addState(int sensor, boolean enabled) {
            this.mIndividualEnabled.put(sensor, new SensorState(enabled));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void upgrade() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PVersion1 {
        private SparseArray<SparseArray<SensorState>> mIndividualEnabled;

        private PVersion1(int version) {
            this.mIndividualEnabled = new SparseArray<>();
            if (version != 1) {
                throw new RuntimeException("Only version 1 supported");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static PVersion1 fromPVersion0(PVersion0 version0) {
            version0.upgrade();
            PVersion1 result = new PVersion1(1);
            int[] users = {0};
            try {
                users = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).getUserIds();
            } catch (Exception e) {
                Log.e(PersistedState.LOG_TAG, "Unable to get users.", e);
            }
            for (int userId : users) {
                for (int j = 0; j < version0.mIndividualEnabled.size(); j++) {
                    int sensor = version0.mIndividualEnabled.keyAt(j);
                    SensorState sensorState = (SensorState) version0.mIndividualEnabled.valueAt(j);
                    result.addState(userId, sensor, sensorState.isEnabled());
                }
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addState(int userId, int sensor, boolean enabled) {
            SparseArray<SensorState> userIndividualSensorEnabled = this.mIndividualEnabled.get(userId, new SparseArray<>());
            this.mIndividualEnabled.put(userId, userIndividualSensorEnabled);
            userIndividualSensorEnabled.put(sensor, new SensorState(enabled));
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void upgrade() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PVersion2 {
        private ArrayMap<TypeUserSensor, SensorState> mStates;

        private PVersion2(int version) {
            this.mStates = new ArrayMap<>();
            if (version != 2) {
                throw new RuntimeException("Only version 2 supported");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static PVersion2 fromPVersion1(PVersion1 version1) {
            version1.upgrade();
            PVersion2 result = new PVersion2(2);
            SparseArray<SparseArray<SensorState>> individualEnabled = version1.mIndividualEnabled;
            int numUsers = individualEnabled.size();
            for (int i = 0; i < numUsers; i++) {
                int userId = individualEnabled.keyAt(i);
                SparseArray<SensorState> userIndividualEnabled = individualEnabled.valueAt(i);
                int numSensors = userIndividualEnabled.size();
                for (int j = 0; j < numSensors; j++) {
                    int sensor = userIndividualEnabled.keyAt(j);
                    SensorState sensorState = userIndividualEnabled.valueAt(j);
                    result.addState(1, userId, sensor, sensorState.getState(), sensorState.getLastChange());
                }
            }
            return result;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addState(int toggleType, int userId, int sensor, int state, long lastChange) {
            this.mStates.put(new TypeUserSensor(toggleType, userId, sensor), new SensorState(state, lastChange));
        }
    }

    public void resetForTesting() {
        this.mStates = new ArrayMap<>();
    }
}
