package com.android.server.display.config;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Thresholds {
    private BrightnessThresholds brighteningThresholds;
    private BrightnessThresholds darkeningThresholds;

    public final BrightnessThresholds getBrighteningThresholds() {
        return this.brighteningThresholds;
    }

    boolean hasBrighteningThresholds() {
        if (this.brighteningThresholds == null) {
            return false;
        }
        return true;
    }

    public final void setBrighteningThresholds(BrightnessThresholds brighteningThresholds) {
        this.brighteningThresholds = brighteningThresholds;
    }

    public final BrightnessThresholds getDarkeningThresholds() {
        return this.darkeningThresholds;
    }

    boolean hasDarkeningThresholds() {
        if (this.darkeningThresholds == null) {
            return false;
        }
        return true;
    }

    public final void setDarkeningThresholds(BrightnessThresholds darkeningThresholds) {
        this.darkeningThresholds = darkeningThresholds;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Thresholds read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Thresholds instance = new Thresholds();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("brighteningThresholds")) {
                    BrightnessThresholds value = BrightnessThresholds.read(parser);
                    instance.setBrighteningThresholds(value);
                } else if (tagName.equals("darkeningThresholds")) {
                    BrightnessThresholds value2 = BrightnessThresholds.read(parser);
                    instance.setDarkeningThresholds(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Thresholds is not closed");
        }
        return instance;
    }
}
