package android.audio.policy.configuration.V7_0;

import android.provider.Telephony;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SurroundFormats {
    private List<Format> format;

    /* loaded from: classes.dex */
    public static class Format {
        private String name;
        private List<String> subformats;

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

        public List<String> getSubformats() {
            if (this.subformats == null) {
                this.subformats = new ArrayList();
            }
            return this.subformats;
        }

        boolean hasSubformats() {
            if (this.subformats == null) {
                return false;
            }
            return true;
        }

        public void setSubformats(List<String> subformats) {
            this.subformats = subformats;
        }

        static Format read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            String[] split;
            Format instance = new Format();
            String raw = parser.getAttributeValue(null, "name");
            if (raw != null) {
                instance.setName(raw);
            }
            String raw2 = parser.getAttributeValue(null, "subformats");
            if (raw2 != null) {
                List<String> value = new ArrayList<>();
                for (String token : raw2.split("\\s+")) {
                    value.add(token);
                }
                instance.setSubformats(value);
            }
            XmlParser.skip(parser);
            return instance;
        }
    }

    public List<Format> getFormat() {
        if (this.format == null) {
            this.format = new ArrayList();
        }
        return this.format;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SurroundFormats read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        SurroundFormats instance = new SurroundFormats();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals(Telephony.CellBroadcasts.MESSAGE_FORMAT)) {
                    Format value = Format.read(parser);
                    instance.getFormat().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("SurroundFormats is not closed");
        }
        return instance;
    }
}
