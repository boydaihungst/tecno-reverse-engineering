package com.android.server.display.config;

import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class BrightnessThresholds {
    private BigDecimal minimum;

    public final BigDecimal getMinimum() {
        return this.minimum;
    }

    boolean hasMinimum() {
        if (this.minimum == null) {
            return false;
        }
        return true;
    }

    public final void setMinimum(BigDecimal minimum) {
        this.minimum = minimum;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BrightnessThresholds read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        BrightnessThresholds instance = new BrightnessThresholds();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("minimum")) {
                    String raw = XmlParser.readText(parser);
                    BigDecimal value = new BigDecimal(raw);
                    instance.setMinimum(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("BrightnessThresholds is not closed");
        }
        return instance;
    }
}
