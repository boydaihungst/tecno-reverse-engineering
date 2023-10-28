package com.android.server.policy.devicestate.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class Flags {
    private List<String> flag;

    public List<String> getFlag() {
        if (this.flag == null) {
            this.flag = new ArrayList();
        }
        return this.flag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Flags read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Flags instance = new Flags();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("flag")) {
                    String raw = XmlParser.readText(parser);
                    instance.getFlag().add(raw);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Flags is not closed");
        }
        return instance;
    }
}
