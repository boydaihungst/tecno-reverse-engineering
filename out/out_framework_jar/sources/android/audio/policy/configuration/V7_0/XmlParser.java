package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
/* loaded from: classes.dex */
public class XmlParser {
    public static AudioPolicyConfiguration read(InputStream in) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true);
        parser.setInput(in, null);
        parser.nextTag();
        String tagName = parser.getName();
        if (!tagName.equals("audioPolicyConfiguration")) {
            return null;
        }
        AudioPolicyConfiguration value = AudioPolicyConfiguration.read(parser);
        return value;
    }

    public static String readText(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.next() != 4) {
            return "";
        }
        String result = parser.getText();
        parser.nextTag();
        return result;
    }

    public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case 2:
                    depth++;
                    break;
                case 3:
                    depth--;
                    break;
            }
        }
    }
}
