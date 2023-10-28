package com.android.server.display.config;

import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class BrightnessThrottlingPoint {
    private BigDecimal brightness;
    private ThermalStatus thermalStatus;

    public final ThermalStatus getThermalStatus() {
        return this.thermalStatus;
    }

    boolean hasThermalStatus() {
        if (this.thermalStatus == null) {
            return false;
        }
        return true;
    }

    public final void setThermalStatus(ThermalStatus thermalStatus) {
        this.thermalStatus = thermalStatus;
    }

    public final BigDecimal getBrightness() {
        return this.brightness;
    }

    boolean hasBrightness() {
        if (this.brightness == null) {
            return false;
        }
        return true;
    }

    public final void setBrightness(BigDecimal brightness) {
        this.brightness = brightness;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BrightnessThrottlingPoint read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        BrightnessThrottlingPoint instance = new BrightnessThrottlingPoint();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("thermalStatus")) {
                    String raw = XmlParser.readText(parser);
                    ThermalStatus value = ThermalStatus.fromString(raw);
                    instance.setThermalStatus(value);
                } else if (tagName.equals("brightness")) {
                    String raw2 = XmlParser.readText(parser);
                    BigDecimal value2 = new BigDecimal(raw2);
                    instance.setBrightness(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("BrightnessThrottlingPoint is not closed");
        }
        return instance;
    }
}
