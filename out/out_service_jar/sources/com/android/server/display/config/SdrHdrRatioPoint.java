package com.android.server.display.config;

import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class SdrHdrRatioPoint {
    private BigDecimal hdrRatio;
    private BigDecimal sdrNits;

    public final BigDecimal getSdrNits() {
        return this.sdrNits;
    }

    boolean hasSdrNits() {
        if (this.sdrNits == null) {
            return false;
        }
        return true;
    }

    public final void setSdrNits(BigDecimal sdrNits) {
        this.sdrNits = sdrNits;
    }

    public final BigDecimal getHdrRatio() {
        return this.hdrRatio;
    }

    boolean hasHdrRatio() {
        if (this.hdrRatio == null) {
            return false;
        }
        return true;
    }

    public final void setHdrRatio(BigDecimal hdrRatio) {
        this.hdrRatio = hdrRatio;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SdrHdrRatioPoint read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        SdrHdrRatioPoint instance = new SdrHdrRatioPoint();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("sdrNits")) {
                    String raw = XmlParser.readText(parser);
                    BigDecimal value = new BigDecimal(raw);
                    instance.setSdrNits(value);
                } else if (tagName.equals("hdrRatio")) {
                    String raw2 = XmlParser.readText(parser);
                    BigDecimal value2 = new BigDecimal(raw2);
                    instance.setHdrRatio(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("SdrHdrRatioPoint is not closed");
        }
        return instance;
    }
}
