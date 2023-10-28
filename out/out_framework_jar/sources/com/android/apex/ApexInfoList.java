package com.android.apex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes4.dex */
public class ApexInfoList {
    private List<ApexInfo> apexInfo;

    public List<ApexInfo> getApexInfo() {
        if (this.apexInfo == null) {
            this.apexInfo = new ArrayList();
        }
        return this.apexInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ApexInfoList read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        ApexInfoList instance = new ApexInfoList();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("apex-info")) {
                    ApexInfo value = ApexInfo.read(parser);
                    instance.getApexInfo().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("ApexInfoList is not closed");
        }
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void write(XmlWriter out, String name) throws IOException {
        out.print("<" + name);
        out.print(">\n");
        out.increaseIndent();
        for (ApexInfo value : getApexInfo()) {
            value.write(out, "apex-info");
        }
        out.decreaseIndent();
        out.print("</" + name + ">\n");
    }
}
