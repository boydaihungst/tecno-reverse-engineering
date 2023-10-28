package com.android.server.display.config.layout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Layouts {
    private List<Layout> layout;

    public List<Layout> getLayout() {
        if (this.layout == null) {
            this.layout = new ArrayList();
        }
        return this.layout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Layouts read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Layouts instance = new Layouts();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("layout")) {
                    Layout value = Layout.read(parser);
                    instance.getLayout().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Layouts is not closed");
        }
        return instance;
    }
}
