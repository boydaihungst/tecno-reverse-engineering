package com.android.server.policy.devicestate.config;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class LidSwitchCondition {
    private Boolean open;

    public boolean getOpen() {
        Boolean bool = this.open;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    boolean hasOpen() {
        if (this.open == null) {
            return false;
        }
        return true;
    }

    public void setOpen(boolean open) {
        this.open = Boolean.valueOf(open);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static LidSwitchCondition read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        LidSwitchCondition instance = new LidSwitchCondition();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("open")) {
                    String raw = XmlParser.readText(parser);
                    boolean value = Boolean.parseBoolean(raw);
                    instance.setOpen(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("LidSwitchCondition is not closed");
        }
        return instance;
    }
}
