package com.android.server.display.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SdrHdrRatioMap {
    private List<SdrHdrRatioPoint> point;

    public final List<SdrHdrRatioPoint> getPoint() {
        if (this.point == null) {
            this.point = new ArrayList();
        }
        return this.point;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SdrHdrRatioMap read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        SdrHdrRatioMap instance = new SdrHdrRatioMap();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("point")) {
                    SdrHdrRatioPoint value = SdrHdrRatioPoint.read(parser);
                    instance.getPoint().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("SdrHdrRatioMap is not closed");
        }
        return instance;
    }
}
