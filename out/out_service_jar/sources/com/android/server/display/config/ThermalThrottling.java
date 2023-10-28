package com.android.server.display.config;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class ThermalThrottling {
    private BrightnessThrottlingMap brightnessThrottlingMap;

    public final BrightnessThrottlingMap getBrightnessThrottlingMap() {
        return this.brightnessThrottlingMap;
    }

    boolean hasBrightnessThrottlingMap() {
        if (this.brightnessThrottlingMap == null) {
            return false;
        }
        return true;
    }

    public final void setBrightnessThrottlingMap(BrightnessThrottlingMap brightnessThrottlingMap) {
        this.brightnessThrottlingMap = brightnessThrottlingMap;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ThermalThrottling read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        ThermalThrottling instance = new ThermalThrottling();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("brightnessThrottlingMap")) {
                    BrightnessThrottlingMap value = BrightnessThrottlingMap.read(parser);
                    instance.setBrightnessThrottlingMap(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("ThermalThrottling is not closed");
        }
        return instance;
    }
}
