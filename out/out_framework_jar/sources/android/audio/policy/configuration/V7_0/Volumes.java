package android.audio.policy.configuration.V7_0;

import android.speech.tts.TextToSpeech;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Volumes {
    private List<Reference> reference;
    private List<Volume> volume;

    public List<Volume> getVolume() {
        if (this.volume == null) {
            this.volume = new ArrayList();
        }
        return this.volume;
    }

    public List<Reference> getReference() {
        if (this.reference == null) {
            this.reference = new ArrayList();
        }
        return this.reference;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Volumes read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Volumes instance = new Volumes();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals(TextToSpeech.Engine.KEY_PARAM_VOLUME)) {
                    Volume value = Volume.read(parser);
                    instance.getVolume().add(value);
                } else if (tagName.equals("reference")) {
                    Reference value2 = Reference.read(parser);
                    instance.getReference().add(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Volumes is not closed");
        }
        return instance;
    }
}
