package com.android.server.policy.devicestate.config;

import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public class NumericRange {
    private BigDecimal maxInclusive_optional;
    private BigDecimal max_optional;
    private BigDecimal minInclusive_optional;
    private BigDecimal min_optional;

    public BigDecimal getMin_optional() {
        return this.min_optional;
    }

    boolean hasMin_optional() {
        if (this.min_optional == null) {
            return false;
        }
        return true;
    }

    public void setMin_optional(BigDecimal min_optional) {
        this.min_optional = min_optional;
    }

    public BigDecimal getMinInclusive_optional() {
        return this.minInclusive_optional;
    }

    boolean hasMinInclusive_optional() {
        if (this.minInclusive_optional == null) {
            return false;
        }
        return true;
    }

    public void setMinInclusive_optional(BigDecimal minInclusive_optional) {
        this.minInclusive_optional = minInclusive_optional;
    }

    public BigDecimal getMax_optional() {
        return this.max_optional;
    }

    boolean hasMax_optional() {
        if (this.max_optional == null) {
            return false;
        }
        return true;
    }

    public void setMax_optional(BigDecimal max_optional) {
        this.max_optional = max_optional;
    }

    public BigDecimal getMaxInclusive_optional() {
        return this.maxInclusive_optional;
    }

    boolean hasMaxInclusive_optional() {
        if (this.maxInclusive_optional == null) {
            return false;
        }
        return true;
    }

    public void setMaxInclusive_optional(BigDecimal maxInclusive_optional) {
        this.maxInclusive_optional = maxInclusive_optional;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NumericRange read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        NumericRange instance = new NumericRange();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("min")) {
                    String raw = XmlParser.readText(parser);
                    BigDecimal value = new BigDecimal(raw);
                    instance.setMin_optional(value);
                } else if (tagName.equals("min-inclusive")) {
                    String raw2 = XmlParser.readText(parser);
                    BigDecimal value2 = new BigDecimal(raw2);
                    instance.setMinInclusive_optional(value2);
                } else if (tagName.equals("max")) {
                    String raw3 = XmlParser.readText(parser);
                    BigDecimal value3 = new BigDecimal(raw3);
                    instance.setMax_optional(value3);
                } else if (tagName.equals("max-inclusive")) {
                    String raw4 = XmlParser.readText(parser);
                    BigDecimal value4 = new BigDecimal(raw4);
                    instance.setMaxInclusive_optional(value4);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("NumericRange is not closed");
        }
        return instance;
    }
}
