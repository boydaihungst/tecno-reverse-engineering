package com.android.server.display.config.layout;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Layout {
    private List<Display> display;
    private BigInteger state;

    public BigInteger getState() {
        return this.state;
    }

    boolean hasState() {
        if (this.state == null) {
            return false;
        }
        return true;
    }

    public void setState(BigInteger state) {
        this.state = state;
    }

    public List<Display> getDisplay() {
        if (this.display == null) {
            this.display = new ArrayList();
        }
        return this.display;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Layout read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Layout instance = new Layout();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("state")) {
                    String raw = XmlParser.readText(parser);
                    BigInteger value = new BigInteger(raw);
                    instance.setState(value);
                } else if (tagName.equals("display")) {
                    Display value2 = Display.read(parser);
                    instance.getDisplay().add(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Layout is not closed");
        }
        return instance;
    }
}
