package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SurroundSound {
    private SurroundFormats formats;

    public SurroundFormats getFormats() {
        return this.formats;
    }

    boolean hasFormats() {
        if (this.formats == null) {
            return false;
        }
        return true;
    }

    public void setFormats(SurroundFormats formats) {
        this.formats = formats;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SurroundSound read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        SurroundSound instance = new SurroundSound();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("formats")) {
                    SurroundFormats value = SurroundFormats.read(parser);
                    instance.setFormats(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("SurroundSound is not closed");
        }
        return instance;
    }
}
