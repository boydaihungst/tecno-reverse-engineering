package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class AudioPolicyConfiguration {
    private GlobalConfiguration globalConfiguration;
    private List<Modules> modules;
    private SurroundSound surroundSound;
    private Version version;
    private List<Volumes> volumes;

    public GlobalConfiguration getGlobalConfiguration() {
        return this.globalConfiguration;
    }

    boolean hasGlobalConfiguration() {
        if (this.globalConfiguration == null) {
            return false;
        }
        return true;
    }

    public void setGlobalConfiguration(GlobalConfiguration globalConfiguration) {
        this.globalConfiguration = globalConfiguration;
    }

    public List<Modules> getModules() {
        if (this.modules == null) {
            this.modules = new ArrayList();
        }
        return this.modules;
    }

    public List<Volumes> getVolumes() {
        if (this.volumes == null) {
            this.volumes = new ArrayList();
        }
        return this.volumes;
    }

    public SurroundSound getSurroundSound() {
        return this.surroundSound;
    }

    boolean hasSurroundSound() {
        if (this.surroundSound == null) {
            return false;
        }
        return true;
    }

    public void setSurroundSound(SurroundSound surroundSound) {
        this.surroundSound = surroundSound;
    }

    public Version getVersion() {
        return this.version;
    }

    boolean hasVersion() {
        if (this.version == null) {
            return false;
        }
        return true;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AudioPolicyConfiguration read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        AudioPolicyConfiguration instance = new AudioPolicyConfiguration();
        String raw = parser.getAttributeValue(null, "version");
        if (raw != null) {
            Version value = Version.fromString(raw);
            instance.setVersion(value);
        }
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("globalConfiguration")) {
                    GlobalConfiguration value2 = GlobalConfiguration.read(parser);
                    instance.setGlobalConfiguration(value2);
                } else if (tagName.equals("modules")) {
                    Modules value3 = Modules.read(parser);
                    instance.getModules().add(value3);
                } else if (tagName.equals("volumes")) {
                    Volumes value4 = Volumes.read(parser);
                    instance.getVolumes().add(value4);
                } else if (tagName.equals("surroundSound")) {
                    SurroundSound value5 = SurroundSound.read(parser);
                    instance.setSurroundSound(value5);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("AudioPolicyConfiguration is not closed");
        }
        return instance;
    }
}
