package com.android.server.display.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class BrightnessThrottlingMap {
    private List<BrightnessThrottlingPoint> brightnessThrottlingPoint;

    public final List<BrightnessThrottlingPoint> getBrightnessThrottlingPoint() {
        if (this.brightnessThrottlingPoint == null) {
            this.brightnessThrottlingPoint = new ArrayList();
        }
        return this.brightnessThrottlingPoint;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BrightnessThrottlingMap read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        BrightnessThrottlingMap instance = new BrightnessThrottlingMap();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("brightnessThrottlingPoint")) {
                    BrightnessThrottlingPoint value = BrightnessThrottlingPoint.read(parser);
                    instance.getBrightnessThrottlingPoint().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("BrightnessThrottlingMap is not closed");
        }
        return instance;
    }
}
