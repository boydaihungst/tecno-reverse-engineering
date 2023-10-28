package com.android.server.display.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class DisplayQuirks {
    private List<String> quirk;

    public List<String> getQuirk() {
        if (this.quirk == null) {
            this.quirk = new ArrayList();
        }
        return this.quirk;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DisplayQuirks read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        DisplayQuirks instance = new DisplayQuirks();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("quirk")) {
                    String raw = XmlParser.readText(parser);
                    instance.getQuirk().add(raw);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("DisplayQuirks is not closed");
        }
        return instance;
    }
}
