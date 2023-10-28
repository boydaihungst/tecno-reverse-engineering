package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class GlobalConfiguration {
    private Boolean call_screen_mode_supported;
    private EngineSuffix engine_library;
    private Boolean speaker_drc_enabled;

    public boolean getSpeaker_drc_enabled() {
        Boolean bool = this.speaker_drc_enabled;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasSpeaker_drc_enabled() {
        if (this.speaker_drc_enabled == null) {
            return false;
        }
        return true;
    }

    public void setSpeaker_drc_enabled(boolean speaker_drc_enabled) {
        this.speaker_drc_enabled = Boolean.valueOf(speaker_drc_enabled);
    }

    public boolean getCall_screen_mode_supported() {
        Boolean bool = this.call_screen_mode_supported;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasCall_screen_mode_supported() {
        if (this.call_screen_mode_supported == null) {
            return false;
        }
        return true;
    }

    public void setCall_screen_mode_supported(boolean call_screen_mode_supported) {
        this.call_screen_mode_supported = Boolean.valueOf(call_screen_mode_supported);
    }

    public EngineSuffix getEngine_library() {
        return this.engine_library;
    }

    boolean hasEngine_library() {
        if (this.engine_library == null) {
            return false;
        }
        return true;
    }

    public void setEngine_library(EngineSuffix engine_library) {
        this.engine_library = engine_library;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static GlobalConfiguration read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        GlobalConfiguration instance = new GlobalConfiguration();
        String raw = parser.getAttributeValue(null, "speaker_drc_enabled");
        if (raw != null) {
            boolean value = Boolean.parseBoolean(raw);
            instance.setSpeaker_drc_enabled(value);
        }
        String raw2 = parser.getAttributeValue(null, "call_screen_mode_supported");
        if (raw2 != null) {
            boolean value2 = Boolean.parseBoolean(raw2);
            instance.setCall_screen_mode_supported(value2);
        }
        String raw3 = parser.getAttributeValue(null, "engine_library");
        if (raw3 != null) {
            EngineSuffix value3 = EngineSuffix.fromString(raw3);
            instance.setEngine_library(value3);
        }
        XmlParser.skip(parser);
        return instance;
    }
}
