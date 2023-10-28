package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Volume {
    private DeviceCategory deviceCategory;
    private List<String> point;
    private String ref;
    private AudioStreamType stream;

    public List<String> getPoint() {
        if (this.point == null) {
            this.point = new ArrayList();
        }
        return this.point;
    }

    public AudioStreamType getStream() {
        return this.stream;
    }

    boolean hasStream() {
        if (this.stream == null) {
            return false;
        }
        return true;
    }

    public void setStream(AudioStreamType stream) {
        this.stream = stream;
    }

    public DeviceCategory getDeviceCategory() {
        return this.deviceCategory;
    }

    boolean hasDeviceCategory() {
        if (this.deviceCategory == null) {
            return false;
        }
        return true;
    }

    public void setDeviceCategory(DeviceCategory deviceCategory) {
        this.deviceCategory = deviceCategory;
    }

    public String getRef() {
        return this.ref;
    }

    boolean hasRef() {
        if (this.ref == null) {
            return false;
        }
        return true;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Volume read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Volume instance = new Volume();
        String raw = parser.getAttributeValue(null, "stream");
        if (raw != null) {
            AudioStreamType value = AudioStreamType.fromString(raw);
            instance.setStream(value);
        }
        String raw2 = parser.getAttributeValue(null, "deviceCategory");
        if (raw2 != null) {
            DeviceCategory value2 = DeviceCategory.fromString(raw2);
            instance.setDeviceCategory(value2);
        }
        String raw3 = parser.getAttributeValue(null, "ref");
        if (raw3 != null) {
            instance.setRef(raw3);
        }
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("point")) {
                    instance.getPoint().add(XmlParser.readText(parser));
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Volume is not closed");
        }
        return instance;
    }
}
