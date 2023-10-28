package com.android.server.display.config;

import java.io.IOException;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class HbmTiming {
    private BigInteger timeMaxSecs_all;
    private BigInteger timeMinSecs_all;
    private BigInteger timeWindowSecs_all;

    public final BigInteger getTimeWindowSecs_all() {
        return this.timeWindowSecs_all;
    }

    boolean hasTimeWindowSecs_all() {
        if (this.timeWindowSecs_all == null) {
            return false;
        }
        return true;
    }

    public final void setTimeWindowSecs_all(BigInteger timeWindowSecs_all) {
        this.timeWindowSecs_all = timeWindowSecs_all;
    }

    public final BigInteger getTimeMaxSecs_all() {
        return this.timeMaxSecs_all;
    }

    boolean hasTimeMaxSecs_all() {
        if (this.timeMaxSecs_all == null) {
            return false;
        }
        return true;
    }

    public final void setTimeMaxSecs_all(BigInteger timeMaxSecs_all) {
        this.timeMaxSecs_all = timeMaxSecs_all;
    }

    public final BigInteger getTimeMinSecs_all() {
        return this.timeMinSecs_all;
    }

    boolean hasTimeMinSecs_all() {
        if (this.timeMinSecs_all == null) {
            return false;
        }
        return true;
    }

    public final void setTimeMinSecs_all(BigInteger timeMinSecs_all) {
        this.timeMinSecs_all = timeMinSecs_all;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HbmTiming read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        HbmTiming instance = new HbmTiming();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("timeWindowSecs")) {
                    String raw = XmlParser.readText(parser);
                    BigInteger value = new BigInteger(raw);
                    instance.setTimeWindowSecs_all(value);
                } else if (tagName.equals("timeMaxSecs")) {
                    String raw2 = XmlParser.readText(parser);
                    BigInteger value2 = new BigInteger(raw2);
                    instance.setTimeMaxSecs_all(value2);
                } else if (tagName.equals("timeMinSecs")) {
                    String raw3 = XmlParser.readText(parser);
                    BigInteger value3 = new BigInteger(raw3);
                    instance.setTimeMinSecs_all(value3);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("HbmTiming is not closed");
        }
        return instance;
    }
}
