package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Reference {
    private String name;
    private List<String> point;

    public List<String> getPoint() {
        if (this.point == null) {
            this.point = new ArrayList();
        }
        return this.point;
    }

    public String getName() {
        return this.name;
    }

    boolean hasName() {
        if (this.name == null) {
            return false;
        }
        return true;
    }

    public void setName(String name) {
        this.name = name;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Reference read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Reference instance = new Reference();
        String raw = parser.getAttributeValue(null, "name");
        if (raw != null) {
            instance.setName(raw);
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
            throw new DatatypeConfigurationException("Reference is not closed");
        }
        return instance;
    }
}
