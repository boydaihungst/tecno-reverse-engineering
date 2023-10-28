package com.android.server.compat.overrides;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Overrides {
    private List<ChangeOverrides> changeOverrides;

    public List<ChangeOverrides> getChangeOverrides() {
        if (this.changeOverrides == null) {
            this.changeOverrides = new ArrayList();
        }
        return this.changeOverrides;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Overrides read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Overrides instance = new Overrides();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("change-overrides")) {
                    ChangeOverrides value = ChangeOverrides.read(parser);
                    instance.getChangeOverrides().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Overrides is not closed");
        }
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(XmlWriter out, String name) throws IOException {
        out.print("<" + name);
        out.print(">\n");
        out.increaseIndent();
        for (ChangeOverrides value : getChangeOverrides()) {
            value.write(out, "change-overrides");
        }
        out.decreaseIndent();
        out.print("</" + name + ">\n");
    }
}
