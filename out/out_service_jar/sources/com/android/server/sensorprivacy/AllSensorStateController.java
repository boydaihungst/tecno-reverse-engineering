package com.android.server.sensorprivacy;

import android.os.Environment;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.Log;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.IoThread;
import com.android.server.sensorprivacy.SensorPrivacyStateController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AllSensorStateController {
    private static final String LOG_TAG = AllSensorStateController.class.getSimpleName();
    private static final String SENSOR_PRIVACY_XML_FILE = "sensor_privacy.xml";
    private static final String XML_ATTRIBUTE_ENABLED = "enabled";
    private static final String XML_TAG_SENSOR_PRIVACY = "all-sensor-privacy";
    private static final String XML_TAG_SENSOR_PRIVACY_LEGACY = "sensor-privacy";
    private static AllSensorStateController sInstance;
    private final AtomicFile mAtomicFile;
    private boolean mEnabled;
    private SensorPrivacyStateController.AllSensorPrivacyListener mListener;
    private Handler mListenerHandler;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AllSensorStateController getInstance() {
        if (sInstance == null) {
            sInstance = new AllSensorStateController();
        }
        return sInstance;
    }

    private AllSensorStateController() {
        AtomicFile atomicFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), SENSOR_PRIVACY_XML_FILE));
        this.mAtomicFile = atomicFile;
        if (!atomicFile.exists()) {
            return;
        }
        try {
            FileInputStream inputStream = atomicFile.openRead();
            TypedXmlPullParser parser = Xml.resolvePullParser(inputStream);
            while (true) {
                if (parser.getEventType() == 1) {
                    break;
                }
                String tagName = parser.getName();
                if (XML_TAG_SENSOR_PRIVACY.equals(tagName)) {
                    this.mEnabled |= XmlUtils.readBooleanAttribute(parser, "enabled", false);
                    break;
                }
                if (XML_TAG_SENSOR_PRIVACY_LEGACY.equals(tagName)) {
                    this.mEnabled |= XmlUtils.readBooleanAttribute(parser, "enabled", false);
                }
                if ("user".equals(tagName)) {
                    int user = XmlUtils.readIntAttribute(parser, "id", -1);
                    if (user == 0) {
                        this.mEnabled = XmlUtils.readBooleanAttribute(parser, "enabled") | this.mEnabled;
                    }
                }
                XmlUtils.nextElement(parser);
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e(LOG_TAG, "Caught an exception reading the state from storage: ", e);
            this.mEnabled = false;
        }
    }

    public boolean getAllSensorStateLocked() {
        return this.mEnabled;
    }

    public void setAllSensorStateLocked(boolean enabled) {
        Handler handler;
        if (this.mEnabled != enabled) {
            this.mEnabled = enabled;
            final SensorPrivacyStateController.AllSensorPrivacyListener allSensorPrivacyListener = this.mListener;
            if (allSensorPrivacyListener != null && (handler = this.mListenerHandler) != null) {
                Objects.requireNonNull(allSensorPrivacyListener);
                handler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.sensorprivacy.AllSensorStateController$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        SensorPrivacyStateController.AllSensorPrivacyListener.this.onAllSensorPrivacyChanged(((Boolean) obj).booleanValue());
                    }
                }, Boolean.valueOf(enabled)));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllSensorPrivacyListenerLocked(Handler handler, SensorPrivacyStateController.AllSensorPrivacyListener listener) {
        Objects.requireNonNull(handler);
        Objects.requireNonNull(listener);
        if (this.mListener != null) {
            throw new IllegalStateException("Listener is already set");
        }
        this.mListener = listener;
        this.mListenerHandler = handler;
    }

    public void schedulePersistLocked() {
        IoThread.getHandler().sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.sensorprivacy.AllSensorStateController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AllSensorStateController.this.persist(((Boolean) obj).booleanValue());
            }
        }, Boolean.valueOf(this.mEnabled)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void persist(boolean enabled) {
        FileOutputStream outputStream = null;
        try {
            outputStream = this.mAtomicFile.startWrite();
            TypedXmlSerializer serializer = Xml.resolveSerializer(outputStream);
            serializer.startDocument((String) null, true);
            serializer.startTag((String) null, XML_TAG_SENSOR_PRIVACY);
            serializer.attributeBoolean((String) null, "enabled", enabled);
            serializer.endTag((String) null, XML_TAG_SENSOR_PRIVACY);
            serializer.endDocument();
            this.mAtomicFile.finishWrite(outputStream);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Caught an exception persisting the sensor privacy state: ", e);
            this.mAtomicFile.failWrite(outputStream);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetForTesting() {
        this.mListener = null;
        this.mListenerHandler = null;
        this.mEnabled = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLocked(DualDumpOutputStream dumpStream) {
    }
}
