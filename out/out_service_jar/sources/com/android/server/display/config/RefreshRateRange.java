package com.android.server.display.config;

import java.io.IOException;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class RefreshRateRange {
    private BigInteger maximum;
    private BigInteger minimum;

    public final BigInteger getMinimum() {
        return this.minimum;
    }

    boolean hasMinimum() {
        if (this.minimum == null) {
            return false;
        }
        return true;
    }

    public final void setMinimum(BigInteger minimum) {
        this.minimum = minimum;
    }

    public final BigInteger getMaximum() {
        return this.maximum;
    }

    boolean hasMaximum() {
        if (this.maximum == null) {
            return false;
        }
        return true;
    }

    public final void setMaximum(BigInteger maximum) {
        this.maximum = maximum;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RefreshRateRange read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        RefreshRateRange instance = new RefreshRateRange();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("minimum")) {
                    String raw = XmlParser.readText(parser);
                    BigInteger value = new BigInteger(raw);
                    instance.setMinimum(value);
                } else if (tagName.equals("maximum")) {
                    String raw2 = XmlParser.readText(parser);
                    BigInteger value2 = new BigInteger(raw2);
                    instance.setMaximum(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("RefreshRateRange is not closed");
        }
        return instance;
    }
}
